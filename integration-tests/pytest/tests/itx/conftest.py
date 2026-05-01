import os
import subprocess
from pathlib import Path
from typing import AsyncGenerator, Iterator

import pytest
from sqlalchemy.ext.asyncio import create_async_engine

from itx_testkit.profile import ArtifactProfile
from itx_testkit.seeder.db.base import DbSeeder
from itx_testkit.seeder.db.mariadb import MariaDbDbSeeder
from itx_testkit.seeder.db.postgres import PostgresDbSeeder

# ----------------------------------------
# Artifacts
# ----------------------------------------

repo_root = Path(__file__).parent / "../../../.."
artifact_profiles: dict[str, ArtifactProfile] = {
    "rust": ArtifactProfile(
        cwd=repo_root / "itx-rs",
        build_cmd=["cargo", "build"],
        backend_binary="target/debug/itx-backend",
    ),
    "golang": ArtifactProfile(
        cwd=repo_root / "itx-go",
        build_cmd=["make", "build"],
        backend_binary="bin/itx-backend",
    ),
}

itx_lang = os.environ.get("ITX_LANG", "rust")
if itx_lang not in artifact_profiles:
    raise ValueError(f"ITX_LANG must be one of {sorted(artifact_profiles)}; got {itx_lang!r}")

artifact_profile = artifact_profiles[itx_lang]

# ----------------------------------------
# Docker Compose
# ----------------------------------------

compose_dir = Path(__file__).parent / "../../.."
itx_test_profile = os.environ.get("ITX_TEST_PROFILE", "aws")


def _get_host_port(service: str, *, container_port: int) -> str:
    for cmd in (["docker", "compose"], ["docker-compose"]):
        result = subprocess.run(
            [*cmd, "port", service, str(container_port)],
            cwd=compose_dir,
            capture_output=True,
            text=True,
        )
        if result.returncode == 0:
            _, port = result.stdout.strip().rsplit(":", 1)
            return port

    raise RuntimeError(f"failed to get host port for {service}:{container_port} — is docker compose running?")


excluded_tables = ["flyway_schema_history"]

if itx_test_profile == "aws":
    postgres_host = "127.0.0.1"
    postgres_port = _get_host_port("postgres", container_port=5432)
    postgres_db_name = "itx-db"
    postgres_user = "itx-admin"
    postgres_password = "itx-admin"
    postgres_url = (
        f"postgresql+asyncpg://{postgres_user}:{postgres_password}@{postgres_host}:{postgres_port}/{postgres_db_name}"
    )

    db_env: dict[str, str] = {
        "ITX_DB_PROVIDER": "postgres",
        "ITX_POSTGRES_HOST": postgres_host,
        "ITX_POSTGRES_PORT": postgres_port,
        "ITX_POSTGRES_DB_NAME": postgres_db_name,
        "ITX_POSTGRES_USER": postgres_user,
        "ITX_POSTGRES_PASSWORD": postgres_password,
    }
    db_seeder: DbSeeder = PostgresDbSeeder(
        engine=create_async_engine(url=postgres_url), excluded_tables=excluded_tables
    )

elif itx_test_profile == "onprem":
    mariadb_host = "127.0.0.1"
    mariadb_port = _get_host_port("mariadb", container_port=3306)
    mariadb_db_name = "itx-db"
    mariadb_user = "itx-admin"
    mariadb_password = "itx-admin"
    mariadb_url = f"mysql+asyncmy://{mariadb_user}:{mariadb_password}@{mariadb_host}:{mariadb_port}/{mariadb_db_name}"

    db_env = {
        "ITX_DB_PROVIDER": "mariadb",
        "ITX_MARIADB_HOST": mariadb_host,
        "ITX_MARIADB_PORT": mariadb_port,
        "ITX_MARIADB_DB_NAME": mariadb_db_name,
        "ITX_MARIADB_USER": mariadb_user,
        "ITX_MARIADB_PASSWORD": mariadb_password,
    }
    db_seeder = MariaDbDbSeeder(engine=create_async_engine(url=mariadb_url), excluded_tables=excluded_tables)

else:
    raise ValueError(f"unknown YAAIRT_TEST_PROFILE: {itx_test_profile!r} (expected 'aws' or 'onprem')")


# ----------------------------------------
# Fixtures
# ----------------------------------------


@pytest.fixture(name="artifact_profile", autouse=True, scope="package")
def artifact_profile_fixture() -> Iterator[ArtifactProfile]:
    artifact_profile.build()
    yield artifact_profile


@pytest.fixture(name="control_plane_env", scope="package")
def control_plane_env_fixture() -> Iterator[dict[str, str]]:
    env: dict[str, str] = {
        **db_env,
    }
    yield env


@pytest.fixture(name="compute_plane_env", scope="package")
def compute_plane_env_fixture() -> Iterator[dict[str, str]]:
    env: dict[str, str] = {}
    yield env


@pytest.fixture(name="db_seeder")
async def db_seeder_fixture() -> AsyncGenerator[DbSeeder]:
    async with db_seeder as seeder:
        yield seeder
