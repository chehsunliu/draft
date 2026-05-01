import gzip
import json
from datetime import datetime
from pathlib import Path
from typing import Any

from sqlalchemy import Column, MetaData, Table, insert, text
from sqlalchemy.ext.asyncio import AsyncConnection, AsyncEngine, async_sessionmaker

from itx_testkit.seeder.db.base import DbReader, DbSeeder


class PostgresDbReader(DbReader):
    def __init__(self, engine: AsyncEngine):
        self._session_factory = async_sessionmaker(engine, expire_on_commit=False)

    async def dummy(self):
        pass


class PostgresDbSeeder(DbSeeder):
    def __init__(self, engine: AsyncEngine, excluded_tables: list[str]) -> None:
        self._engine = engine
        self._excluded_tables: set[str] = set(excluded_tables)

        self._metadata = MetaData()
        self._tables: dict[str, Table] = {}

    async def __aenter__(self) -> "PostgresDbSeeder":
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
            concatenated_table_names = ",".join([t.name for t in self._tables.values()])
            await conn.execute(text(f"TRUNCATE TABLE {concatenated_table_names} RESTART IDENTITY CASCADE"))
            await _show_banner(conn, "FINISH RESET TABLES")

    async def write_data(self, folder_path: Path) -> None:
        async with self._engine.begin() as conn:
            # Disable foreign key constraint check.
            await conn.execute(text("SET session_replication_role = 'replica'"))

            for table in self._tables.values():
                rows: list[dict[str, Any]] = []

                gz_file_path = folder_path / f"postgres/{table.name}.json.gz"
                file_path = folder_path / f"postgres/{table.name}.json"

                if gz_file_path.exists():
                    rows = json.loads(gzip.decompress(gz_file_path.read_bytes()))
                elif file_path.exists():
                    rows = json.loads(file_path.read_text())

                if len(rows) == 0:
                    continue

                rows = [_convert(row, table=table) for row in rows]
                await conn.execute(insert(table), rows)

            await _show_banner(conn, "FINISH LOAD DATA")

    def reader(self) -> PostgresDbReader:
        return PostgresDbReader(engine=self._engine)


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

        if str(column.type) == "TIMESTAMP":
            new_row[column_name] = datetime.fromisoformat(v)
        else:
            new_row[column_name] = v

    return new_row
