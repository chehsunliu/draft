import asyncio
import json
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path
from subprocess import Popen

import pytest
from pytest_httpserver import HTTPServer
from werkzeug.wrappers import Response

from itx_testkit.seeder.db.base import DbSeeder
from itx_testkit.seeder.queue.base import QueueSeeder

alice_id = "11111111-1111-1111-1111-111111111111"
alice_email = "alice@example.com"
bob_email = "bob@example.com"


@pytest.fixture(autouse=True)
async def setup(db_seeder: DbSeeder, queue_seeder: QueueSeeder, datadir: Path):
    await db_seeder.reset_tables()
    await db_seeder.write_data(datadir / "20260504_baseline")
    await queue_seeder.reset()
    yield


class TestPostCreated:
    async def test_emails_each_subscriber_and_marks_notified(
        self,
        db_seeder: DbSeeder,
        queue_seeder: QueueSeeder,
        email_server: HTTPServer,
        worker_proc: Popen[str],
    ):
        # Stand-in for the transactional email vendor.
        email_server.expect_request("/v1/email", method="POST").respond_with_response(Response(status=202))

        before = datetime.now(timezone.utc)

        # Publish a post.created event directly — the backend's publish path is covered separately.
        message = json.dumps({"type": "post.created", "postId": 1, "authorId": alice_id})
        await queue_seeder.publish("control_standard", message)

        # Wait up to 25s for the worker to consume and call the email API. ElasticMQ + AWS SDK
        # long-poll occasionally takes a full 20s cycle to surface a freshly-published message.
        deadline = time.time() + 25
        while time.time() < deadline:
            if email_server.log:
                break
            await asyncio.sleep(0.1)

        assert len(email_server.log) == 1, f"expected 1 email POST, got {len(email_server.log)}"
        request, _response = email_server.log[0]
        assert request.headers.get("Authorization") == "Bearer test"
        body = json.loads(request.get_data(as_text=True))
        assert body == {
            "to": bob_email,
            "subject": f"{alice_email} just published a new post",
            "body": "Check out the new post: Hello",
        }

        # Worker must stamp posts.notified_at after sending notifications. Poll briefly because
        # the email POST returns before the worker's mark_notified UPDATE has rounded-tripped.
        deadline = time.time() + 5
        stored = await db_seeder.reader().get_post(1, alice_id)
        while stored["notified_at"] is None and time.time() < deadline:
            await asyncio.sleep(0.1)
            stored = await db_seeder.reader().get_post(1, alice_id)
        assert stored["notified_at"] is not None, "worker did not stamp notified_at"

        # MariaDB returns naive datetimes; the backend writes them in UTC (server tz). Postgres
        # TIMESTAMPTZ returns tz-aware. Normalize both to aware UTC before comparing.
        notified = stored["notified_at"]
        if notified.tzinfo is None:
            notified = notified.replace(tzinfo=timezone.utc)
        # Loose bound — guards against stale/seeded values without depending on DB clock skew.
        assert before - timedelta(seconds=5) <= notified <= datetime.now(timezone.utc) + timedelta(seconds=5)
