from pathlib import Path

import httpx
import pytest

from itx_testkit.seeder.db.base import DbSeeder

alice_id = "11111111-1111-1111-1111-111111111111"
alice_email = "alice@example.com"
bob_id = "22222222-2222-2222-2222-222222222222"
missing_id = "99999999-9999-9999-9999-999999999999"

BAD_REQUEST_SELF = {"error": {"message": "cannot subscribe to yourself"}}
BAD_REQUEST_SELF_UNSUB = {"error": {"message": "cannot unsubscribe from yourself"}}
NOT_FOUND_BODY = {"error": {"message": "not found"}}


@pytest.fixture(autouse=True)
async def setup(db_seeder: DbSeeder, datadir: Path):
    await db_seeder.reset_tables()
    await db_seeder.write_data(datadir / "baseline")

    yield


def headers(user_id: str = alice_id, email: str = alice_email) -> dict[str, str]:
    return {"X-Itx-User-Id": user_id, "X-Itx-User-Email": email}


class TestSubscribe:
    async def test_subscribes_to_author(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder):
        r = strict_httpx_client.post(f"/api/v1/users/{bob_id}/subscriptions", headers=headers())
        assert r.status_code == 204, r.text
        assert r.content == b""

        assert await db_seeder.reader().is_subscribed(alice_id, bob_id) is True

    async def test_idempotent(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder):
        r = strict_httpx_client.post(f"/api/v1/users/{bob_id}/subscriptions", headers=headers())
        assert r.status_code == 204, r.text
        r = strict_httpx_client.post(f"/api/v1/users/{bob_id}/subscriptions", headers=headers())
        assert r.status_code == 204, r.text
        assert await db_seeder.reader().is_subscribed(alice_id, bob_id) is True

    async def test_400_for_self_subscribe(self, strict_httpx_client: httpx.Client):
        r = strict_httpx_client.post(f"/api/v1/users/{alice_id}/subscriptions", headers=headers())
        assert r.status_code == 400, r.text
        assert r.json() == BAD_REQUEST_SELF

    async def test_404_for_missing_author(self, strict_httpx_client: httpx.Client):
        r = strict_httpx_client.post(f"/api/v1/users/{missing_id}/subscriptions", headers=headers())
        assert r.status_code == 404, r.text
        assert r.json() == NOT_FOUND_BODY


class TestUnsubscribe:
    async def test_unsubscribes_from_author(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder):
        r = strict_httpx_client.post(f"/api/v1/users/{bob_id}/subscriptions", headers=headers())
        assert r.status_code == 204, r.text
        assert await db_seeder.reader().is_subscribed(alice_id, bob_id) is True

        r = strict_httpx_client.delete(f"/api/v1/users/{bob_id}/subscriptions", headers=headers())
        assert r.status_code == 204, r.text
        assert r.content == b""
        assert await db_seeder.reader().is_subscribed(alice_id, bob_id) is False

    async def test_idempotent_when_not_subscribed(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder):
        r = strict_httpx_client.delete(f"/api/v1/users/{bob_id}/subscriptions", headers=headers())
        assert r.status_code == 204, r.text
        assert await db_seeder.reader().is_subscribed(alice_id, bob_id) is False

    async def test_400_for_self_unsubscribe(self, strict_httpx_client: httpx.Client):
        r = strict_httpx_client.delete(f"/api/v1/users/{alice_id}/subscriptions", headers=headers())
        assert r.status_code == 400, r.text
        assert r.json() == BAD_REQUEST_SELF_UNSUB

    async def test_404_for_missing_author(self, strict_httpx_client: httpx.Client):
        r = strict_httpx_client.delete(f"/api/v1/users/{missing_id}/subscriptions", headers=headers())
        assert r.status_code == 404, r.text
        assert r.json() == NOT_FOUND_BODY
