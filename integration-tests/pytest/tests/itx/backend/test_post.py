import uuid
from pathlib import Path

import httpx
import pytest

from itx_testkit.seeder.db.base import DbSeeder

user_id = "11111111-1111-1111-1111-111111111111"
other_user_id = "22222222-2222-2222-2222-222222222222"

NOT_FOUND_BODY = {"error": {"message": "not found"}}


@pytest.fixture(autouse=True)
async def setup(db_seeder: DbSeeder, datadir: Path):
    await db_seeder.reset_tables()
    await db_seeder.write_data(datadir / "baseline")

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
                        "id": 3,
                        "authorId": user_id,
                        "title": "Weekend recap",
                        "body": "Coffee and code.",
                        "tags": ["life"],
                        "createdAt": "2026-03-17T12:00:00Z",
                    },
                    {
                        "id": 2,
                        "authorId": user_id,
                        "title": "Rust adventures",
                        "body": "Talking about traits.",
                        "tags": ["design", "rust"],
                        "createdAt": "2026-03-16T11:00:00Z",
                    },
                    {
                        "id": 1,
                        "authorId": user_id,
                        "title": "Yeah",
                        "body": "Blah blah blah...",
                        "tags": [],
                        "createdAt": "2026-03-15T10:00:00Z",
                    },
                ]
            }
        }

        r = strict_httpx_client.get("/api/v1/posts", headers=headers, params={"limit": 2, "offset": 1})
        assert r.status_code == 200, r.text
        items = r.json()["data"]["items"]
        assert [item["id"] for item in items] == [2, 1]


class TestGetPost:
    async def test_returns_owned_post(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder, datadir: Path):
        await db_seeder.write_data(datadir / "20260502_simple")

        r = strict_httpx_client.get("/api/v1/posts/2", headers={"X-Itx-User-Id": user_id})
        assert r.status_code == 200, r.text
        assert r.json() == {
            "data": {
                "id": 2,
                "authorId": user_id,
                "title": "Rust adventures",
                "body": "Talking about traits.",
                "tags": ["design", "rust"],
                "createdAt": "2026-03-16T11:00:00Z",
            }
        }

    async def test_404_for_post_not_owned(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder, datadir: Path):
        await db_seeder.write_data(datadir / "20260502_simple")

        r = strict_httpx_client.get("/api/v1/posts/4", headers={"X-Itx-User-Id": user_id})
        assert r.status_code == 404, r.text
        assert r.json() == NOT_FOUND_BODY

    async def test_404_for_missing_post(self, strict_httpx_client: httpx.Client):
        r = strict_httpx_client.get("/api/v1/posts/9999", headers={"X-Itx-User-Id": user_id})
        assert r.status_code == 404, r.text
        assert r.json() == NOT_FOUND_BODY


class TestCreatePost:
    async def test_creates_post_with_tags(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder):
        body = {"title": "Hello", "body": "World", "tags": ["intro", "draft"]}

        r = strict_httpx_client.post("/api/v1/posts", headers={"X-Itx-User-Id": user_id}, json=body)
        assert r.status_code == 201, r.text

        post = r.json()["data"]
        assert r.json() == {
            "data": {
                "id": post["id"],
                "authorId": user_id,
                "title": "Hello",
                "body": "World",
                "tags": post["tags"],
                "createdAt": post["createdAt"],
            }
        }
        assert isinstance(post["id"], int) and post["id"] >= 1
        assert sorted(post["tags"]) == ["draft", "intro"]
        assert isinstance(post["createdAt"], str) and post["createdAt"].endswith("Z")

        stored = await db_seeder.reader().get_post(post["id"], user_id)
        assert stored["title"] == "Hello"
        assert stored["body"] == "World"
        assert sorted(stored["tags"]) == ["draft", "intro"]
        assert stored["author_id"] == uuid.UUID(user_id)

    async def test_creates_post_without_tags(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder):
        body = {"title": "Plain", "body": "No tags"}

        r = strict_httpx_client.post("/api/v1/posts", headers={"X-Itx-User-Id": user_id}, json=body)
        assert r.status_code == 201, r.text

        post = r.json()["data"]
        assert post["tags"] == []

        stored = await db_seeder.reader().get_post(post["id"], user_id)
        assert stored["tags"] == []


class TestUpdatePost:
    async def test_updates_owned_post(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder, datadir: Path):
        await db_seeder.write_data(datadir / "20260502_simple")

        body = {"title": "Updated title", "tags": ["edited"]}
        r = strict_httpx_client.patch("/api/v1/posts/2", headers={"X-Itx-User-Id": user_id}, json=body)
        assert r.status_code == 200, r.text
        assert r.json() == {
            "data": {
                "id": 2,
                "authorId": user_id,
                "title": "Updated title",
                "body": "Talking about traits.",
                "tags": ["edited"],
                "createdAt": "2026-03-16T11:00:00Z",
            }
        }

        stored = await db_seeder.reader().get_post(2, user_id)
        assert stored["title"] == "Updated title"
        assert stored["body"] == "Talking about traits."
        assert stored["tags"] == ["edited"]

    async def test_partial_update_keeps_unchanged_fields(
        self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder, datadir: Path
    ):
        await db_seeder.write_data(datadir / "20260502_simple")

        r = strict_httpx_client.patch("/api/v1/posts/2", headers={"X-Itx-User-Id": user_id}, json={"body": "New body"})
        assert r.status_code == 200, r.text

        stored = await db_seeder.reader().get_post(2, user_id)
        assert stored["title"] == "Rust adventures"
        assert stored["body"] == "New body"
        assert stored["tags"] == ["design", "rust"]

    async def test_404_for_post_not_owned(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder, datadir: Path):
        await db_seeder.write_data(datadir / "20260502_simple")

        r = strict_httpx_client.patch("/api/v1/posts/4", headers={"X-Itx-User-Id": user_id}, json={"title": "hijacked"})
        assert r.status_code == 404, r.text
        assert r.json() == NOT_FOUND_BODY


class TestDeletePost:
    async def test_deletes_owned_post(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder, datadir: Path):
        await db_seeder.write_data(datadir / "20260502_simple")

        r = strict_httpx_client.delete("/api/v1/posts/2", headers={"X-Itx-User-Id": user_id})
        assert r.status_code == 204, r.text
        assert r.content == b""

        r = strict_httpx_client.get("/api/v1/posts/2", headers={"X-Itx-User-Id": user_id})
        assert r.status_code == 404
        assert r.json() == NOT_FOUND_BODY

    async def test_404_for_post_not_owned(self, strict_httpx_client: httpx.Client, db_seeder: DbSeeder, datadir: Path):
        await db_seeder.write_data(datadir / "20260502_simple")

        r = strict_httpx_client.delete("/api/v1/posts/4", headers={"X-Itx-User-Id": user_id})
        assert r.status_code == 404, r.text
        assert r.json() == NOT_FOUND_BODY

        # Confirm post 4 still exists for the other user via reader.
        stored = await db_seeder.reader().get_post(4, other_user_id)
        assert stored["id"] == 4
