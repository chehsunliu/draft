import gzip
import json
import uuid
from datetime import datetime
from pathlib import Path
from typing import Any

from sqlalchemy import Column, MetaData, Table, insert, select, text
from sqlalchemy.ext.asyncio import AsyncConnection, AsyncEngine, async_sessionmaker
from sqlalchemy.orm import selectinload

from itx_testkit.seeder.db.base import DbReader, DbSeeder
from itx_testkit.seeder.db.mariadb_tables import Post, Subscription


class MariaDbDbReader(DbReader):
    def __init__(self, engine: AsyncEngine):
        self._session_factory = async_sessionmaker(engine, expire_on_commit=False)

    async def get_post(self, post_id: int, user_id: Any) -> dict[str, Any]:
        async with self._session_factory() as session:
            stmt = select(Post).options(selectinload(Post.tags)).where(Post.id == post_id, Post.author_id == user_id)
            post = (await session.execute(stmt)).scalar_one()

        return {
            "id": post.id,
            "author_id": uuid.UUID(post.author_id),
            "title": post.title,
            "body": post.body,
            "tags": [t.name for t in post.tags],
            "created_at": post.created_at,
        }

    async def is_subscribed(self, subscriber_id: str, author_id: str) -> bool:
        async with self._session_factory() as session:
            stmt = select(Subscription).where(
                Subscription.subscriber_id == subscriber_id,
                Subscription.author_id == author_id,
            )
            return (await session.execute(stmt)).scalar_one_or_none() is not None


class MariaDbDbSeeder(DbSeeder):
    def __init__(self, engine: AsyncEngine, excluded_tables: list[str]) -> None:
        self._engine = engine
        self._excluded_tables: set[str] = set(excluded_tables)

        self._metadata = MetaData()
        self._tables: dict[str, Table] = {}

    async def __aenter__(self) -> "MariaDbDbSeeder":
        if not self._tables:
            async with self._engine.begin() as conn:
                await conn.run_sync(self._metadata.reflect)
                self._tables = {
                    t.name: t for t in self._metadata.tables.values() if t.name not in self._excluded_tables
                }

        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb) -> None:
        pass

    async def reset_tables(self) -> None:
        async with self._engine.begin() as conn:
            await conn.execute(text("SET FOREIGN_KEY_CHECKS = 0"))
            for table in self._tables.values():
                await conn.execute(text(f"TRUNCATE TABLE {table.name}"))
            await conn.execute(text("SET FOREIGN_KEY_CHECKS = 1"))
            await _show_banner(conn, "FINISH RESET TABLES")

    async def write_data(self, folder_path: Path) -> None:
        async with self._engine.begin() as conn:
            # Disable foreign key constraint check.
            await conn.execute(text("SET FOREIGN_KEY_CHECKS = 0"))

            for table in self._tables.values():
                rows: list[dict[str, Any]] = []

                gz_file_path = folder_path / f"mariadb/{table.name}.json.gz"
                file_path = folder_path / f"mariadb/{table.name}.json"

                if gz_file_path.exists():
                    rows = json.loads(gzip.decompress(gz_file_path.read_bytes()))
                elif file_path.exists():
                    rows = json.loads(file_path.read_text())

                if len(rows) == 0:
                    continue

                rows = [_convert(row, table=table) for row in rows]
                await conn.execute(insert(table), rows)

            await conn.execute(text("SET FOREIGN_KEY_CHECKS = 1"))
            await _show_banner(conn, "FINISH LOAD DATA")

    def reader(self) -> MariaDbDbReader:
        return MariaDbDbReader(engine=self._engine)


async def _show_banner(conn: AsyncConnection, banner: str):
    text1 = "=" * 40 + f" {banner} " + "=" * 40
    text2 = "=" * len(text1)
    await conn.execute(text(f"SELECT '{text2}'"))
    await conn.execute(text(f"SELECT '{text1}'"))
    await conn.execute(text(f"SELECT '{text2}'"))


def _convert(row: dict[str, Any], *, table: Table) -> dict[str, Any]:
    new_row: dict[str, Any] = {}

    for column_name, v in row.items():
        assert column_name in table.columns
        column: Column = table.columns[column_name]

        if v is None:
            new_row[column_name] = v
            continue

        if str(column.type) in ("DATETIME", "TIMESTAMP"):
            new_row[column_name] = datetime.fromisoformat(v)
        else:
            new_row[column_name] = v

    return new_row
