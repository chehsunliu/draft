import os
import subprocess
from pathlib import Path
from typing import AsyncGenerator, Iterator

import boto3
import pytest
from sqlalchemy.ext.asyncio import create_async_engine

from itx_testkit.profile import ArtifactProfile
from itx_testkit.seeder.db.base import DbSeeder
from itx_testkit.seeder.db.mariadb import MariaDbDbSeeder
from itx_testkit.seeder.db.postgres import PostgresDbSeeder
from itx_testkit.seeder.queue.base import QueueSeeder
from itx_testkit.seeder.queue.rabbitmq import RabbitQueueSeeder
from itx_testkit.seeder.queue.sqs import SqsQueueSeeder

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

# Logical key → broker-specific name. Same set for both profiles.
_queue_keys = {
    "control_standard": "test-itx-control-standard",
    "control_premium": "test-itx-control-premium",
    "compute_standard": "test-itx-compute-standard",
    "compute_premium": "test-itx-compute-premium",
}


queue_seeder: QueueSeeder

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

    sqs_port = _get_host_port("sqs", container_port=9324)
    sqs_endpoint = f"http://127.0.0.1:{sqs_port}"
    _bootstrap_sqs = boto3.client(
        "sqs",
        endpoint_url=sqs_endpoint,
        region_name="us-east-1",
        aws_access_key_id="x",
        aws_secret_access_key="x",
    )
    queue_urls = {key: _bootstrap_sqs.get_queue_url(QueueName=name)["QueueUrl"] for key, name in _queue_keys.items()}

    queue_env: dict[str, str] = {
        "ITX_QUEUE_PROVIDER": "sqs",
        "AWS_REGION": "us-east-1",
        "AWS_ACCESS_KEY_ID": "x",
        "AWS_SECRET_ACCESS_KEY": "x",
        "ITX_SQS_LOCAL_ENDPOINT_URL": sqs_endpoint,
        "ITX_SQS_CONTROL_STANDARD_QUEUE_URL": queue_urls["control_standard"],
        "ITX_SQS_CONTROL_PREMIUM_QUEUE_URL": queue_urls["control_premium"],
        "ITX_SQS_COMPUTE_STANDARD_QUEUE_URL": queue_urls["compute_standard"],
        "ITX_SQS_COMPUTE_PREMIUM_QUEUE_URL": queue_urls["compute_premium"],
    }
    queue_seeder = SqsQueueSeeder(endpoint_url=sqs_endpoint, queue_urls=queue_urls)

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

    rabbit_host = "127.0.0.1"
    rabbit_port = _get_host_port("rabbitmq", container_port=5672)
    rabbit_user = "itx-admin"
    rabbit_password = "itx-admin"

    queue_env = {
        "ITX_QUEUE_PROVIDER": "rabbitmq",
        "ITX_RABBITMQ_HOST": rabbit_host,
        "ITX_RABBITMQ_PORT": rabbit_port,
        "ITX_RABBITMQ_USER": rabbit_user,
        "ITX_RABBITMQ_PASSWORD": rabbit_password,
        "ITX_RABBITMQ_CONTROL_STANDARD_QUEUE": _queue_keys["control_standard"],
        "ITX_RABBITMQ_CONTROL_PREMIUM_QUEUE": _queue_keys["control_premium"],
        "ITX_RABBITMQ_COMPUTE_STANDARD_QUEUE": _queue_keys["compute_standard"],
        "ITX_RABBITMQ_COMPUTE_PREMIUM_QUEUE": _queue_keys["compute_premium"],
    }
    queue_seeder = RabbitQueueSeeder(
        url=f"amqp://{rabbit_user}:{rabbit_password}@{rabbit_host}:{rabbit_port}/%2F",
        queue_names=_queue_keys,
    )

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
        **queue_env,
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


@pytest.fixture(name="queue_seeder")
async def queue_seeder_fixture() -> AsyncGenerator[QueueSeeder]:
    async with queue_seeder as seeder:
        yield seeder
