from pathlib import Path

import httpx
import pytest

from itx_testkit.seeder.db.base import DbSeeder

user_id = "11111111-1111-1111-1111-111111111111"


@pytest.fixture(autouse=True)
async def setup(db_seeder: DbSeeder):
    await db_seeder.reset_tables()

    yield


class TestListPosts:
    async def test_listing_posts_works(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder, datadir: Path):
        await db_seeder.write_data(datadir / "20260502_simple")

        headers = {"X-Itx-User-Id": user_id}

        r = strict_httpx_client.get("/api/v1/posts", headers=headers)

        assert r.status_code == 200, r.text
        assert r.json() == {
            "data": {
                "items": [
                    {
                        "id": 1,
                        "authorId": user_id,
                        "title": "Yeah",
                        "body": "Blah blah blah...",
                        "tags": [],
                        "createdAt": "2026-03-15T10:00:00Z",
                    }
                ]
            }
        }
