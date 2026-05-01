import httpx
import pytest

from itx_testkit.seeder.db.base import DbSeeder


@pytest.fixture(autouse=True)
async def setup(db_seeder: DbSeeder):
    await db_seeder.reset_tables()

    yield


class TestListPosts:
    def test_listing_posts_works(self, strict_httpx_client: httpx.Client):
        headers = {"X-Itx-User-Id": "11111111-1111-1111-1111-111111111111"}

        r = strict_httpx_client.get("/api/v1/posts", headers=headers)

        assert r.status_code == 200, r.text
        assert r.json() == {"data": {"items": []}}
