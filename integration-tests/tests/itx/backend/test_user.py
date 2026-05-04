import httpx
import pytest

from itx_testkit.seeder.db.base import DbSeeder

user_id = "11111111-1111-1111-1111-111111111111"
user_email = "alice@example.com"


@pytest.fixture(autouse=True)
async def setup(db_seeder: DbSeeder):
    await db_seeder.reset_tables()

    yield


class TestGetMe:
    async def test_creates_user_on_first_call_and_returns_it(self, strict_httpx_client: httpx.Client):
        headers = {"X-Itx-User-Id": user_id, "X-Itx-User-Email": user_email}

        r = strict_httpx_client.get("/api/v1/users/me", headers=headers)
        assert r.status_code == 200, r.text
        assert r.json() == {"data": {"id": user_id, "email": user_email}}

        # Second call returns the same row (no duplicate insert).
        r = strict_httpx_client.get("/api/v1/users/me", headers=headers)
        assert r.status_code == 200, r.text
        assert r.json() == {"data": {"id": user_id, "email": user_email}}

    async def test_subsequent_call_keeps_original_email(self, strict_httpx_client: httpx.Client):
        first = strict_httpx_client.get(
            "/api/v1/users/me",
            headers={"X-Itx-User-Id": user_id, "X-Itx-User-Email": user_email},
        )
        assert first.status_code == 200, first.text

        second = strict_httpx_client.get(
            "/api/v1/users/me",
            headers={"X-Itx-User-Id": user_id, "X-Itx-User-Email": "alice+changed@example.com"},
        )
        assert second.status_code == 200, second.text
        assert second.json() == {"data": {"id": user_id, "email": user_email}}
