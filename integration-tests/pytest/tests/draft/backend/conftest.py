from pathlib import Path
from subprocess import Popen
from typing import Iterator

import httpx
import pytest
import yaml

from draft_testkit.openapi import OpenAPIValidator
from draft_testkit.utils import read_all_logs


def load_openapi_schema():
    schema_path = Path(__file__).parent / "../../../../../openapi.yaml"
    with open(schema_path) as f:
        schema = yaml.safe_load(f)
    schema["servers"] = [
        {"url": "http://127.0.0.1:18080", "description": "Integration tests"},
        {"url": "http://127.0.0.1:18081", "description": "Integration tests"},
    ]
    return schema


openapi_schema = load_openapi_schema()


# --- httpx client fixtures ---
#
# Two families of clients, each in three validation levels:
#
#   *_with_server_daemon  — yields (client, Popen) so tests can read server logs
#   plain (no suffix)     — yields just the client
#
# Validation levels:
#   strict_*  — validates both requests AND responses against the OpenAPI schema.
#               Use for happy-path tests to enforce full API contract compliance.
#   lenient_* — validates only responses against the OpenAPI schema.
#               Use for tests that intentionally send invalid/malformed requests.
#   (bare)    — no schema validation at all.
#               Use for basic connectivity checks or when validation is not needed.


@pytest.fixture
def httpx_client_with_server_daemon(
    raw_logged_server_daemon: tuple[Popen[str] | None, str],
) -> Iterator[tuple[httpx.Client, Popen[str] | None]]:
    """No schema validation. Exposes the server process for log reading."""
    proc, server_url = raw_logged_server_daemon

    if proc:
        read_all_logs(proc.stdout)

    with httpx.Client(base_url=server_url) as client:
        yield client, proc

    if proc:
        read_all_logs(proc.stdout)


@pytest.fixture
def strict_httpx_client_with_server_daemon(
    raw_logged_server_daemon: tuple[Popen[str] | None, str],
) -> Iterator[tuple[httpx.Client, Popen[str] | None]]:
    """Validates both requests and responses against the OpenAPI schema. Exposes the server process for log reading."""
    proc, server_url = raw_logged_server_daemon
    validator = OpenAPIValidator(openapi_schema)

    if proc:
        read_all_logs(proc.stdout)

    with httpx.Client(base_url=server_url, event_hooks=validator.as_event_hooks) as client:
        yield client, proc

    if proc:
        read_all_logs(proc.stdout)


@pytest.fixture
def lenient_httpx_client_with_server_daemon(
    raw_logged_server_daemon: tuple[Popen[str] | None, str],
) -> Iterator[tuple[httpx.Client, Popen[str] | None]]:
    """Validates only responses against the OpenAPI schema. Exposes the server process for log reading."""
    proc, server_url = raw_logged_server_daemon
    validator = OpenAPIValidator(openapi_schema, validate_request=False)

    if proc:
        read_all_logs(proc.stdout)

    with httpx.Client(base_url=server_url, event_hooks=validator.as_event_hooks) as client:
        yield client, proc

    if proc:
        read_all_logs(proc.stdout)


@pytest.fixture
def strict_httpx_client(raw_server_daemon: str) -> Iterator[httpx.Client]:
    """
    Returns a httpx client that validates both requests and responses against the OpenAPI schema.
    Use this for standard integration tests to ensure API contract compliance.
    """
    validator = OpenAPIValidator(openapi_schema)

    with httpx.Client(base_url=raw_server_daemon, event_hooks=validator.as_event_hooks) as client:
        yield client


@pytest.fixture
def lenient_httpx_client(raw_server_daemon: str) -> Iterator[httpx.Client]:
    """
    Returns a httpx client that validates only responses against the OpenAPI schema.
    Use this to test how the server handles invalid or malformed requests.
    """
    validator = OpenAPIValidator(openapi_schema, validate_request=False)

    with httpx.Client(base_url=raw_server_daemon, event_hooks=validator.as_event_hooks) as client:
        yield client
