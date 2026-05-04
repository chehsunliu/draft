import os
import signal
import time
from subprocess import Popen
from typing import Iterator

import pytest
from pytest_httpserver import HTTPServer

from itx_testkit.profile import ArtifactProfile

itx_lang = os.environ.get("ITX_LANG", "rust")


@pytest.fixture(name="email_server", scope="package")
def email_server_fixture() -> Iterator[HTTPServer]:
    """Stand-in for the transactional email vendor. Worker POSTs to /v1/email."""
    server = HTTPServer(host="127.0.0.1", port=0)
    server.start()
    yield server
    server.stop()


@pytest.fixture(name="worker_proc")
def worker_proc_fixture(
    email_server: HTTPServer,
    control_plane_env: dict[str, str],
    artifact_profile: ArtifactProfile,
) -> Iterator[Popen[str]]:
    if itx_lang != "rust":
        pytest.skip("worker integration tests are rust-only for now")

    email_server.clear()
    env: dict[str, str] = {
        **control_plane_env,
        "ITX_EMAIL_URL": email_server.url_for("/v1/email"),
        "ITX_EMAIL_API_KEY": "test",
        "RUST_LOG": "info",
    }

    proc = artifact_profile.spawn_control_worker(env, capture_stdout=False)
    # Give the worker a moment to open broker connections + start consuming.
    time.sleep(0.8)
    if proc.poll() is not None:
        raise RuntimeError(f"control worker exited early with code {proc.returncode}")

    yield proc

    proc.send_signal(signal.SIGINT)
    try:
        proc.wait(timeout=10)
    except Exception:
        proc.kill()
        proc.wait()
