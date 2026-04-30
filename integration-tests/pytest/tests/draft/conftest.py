import os
import signal
import subprocess
from pathlib import Path
from subprocess import Popen
from typing import Iterator, NamedTuple

import pytest

from draft_testkit.utils import wait_for_port


class BackendProfile(NamedTuple):
    cwd: Path
    build_cmd: list[str]
    binary: str


REPO_ROOT = Path(__file__).parent / "../../../.."

BACKEND_PROFILES: dict[str, BackendProfile] = {
    "rust": BackendProfile(
        cwd=REPO_ROOT / "draft-rs",
        build_cmd=["cargo", "build"],
        binary="target/debug/draft-backend",
    ),
    "golang": BackendProfile(
        cwd=REPO_ROOT / "draft-go",
        build_cmd=["make", "build"],
        binary="bin/draft-backend",
    ),
}

draft_lang = os.environ.get("DRAFT_LANG", "rust")
if draft_lang not in BACKEND_PROFILES:
    raise ValueError(f"DRAFT_LANG must be one of {sorted(BACKEND_PROFILES)}; got {draft_lang!r}")

backend_profile = BACKEND_PROFILES[draft_lang]


def _spawn_backend(
    profile: BackendProfile,
    proc_env: dict[str, str],
    host: str,
    port: int,
    *,
    capture_stdout: bool,
) -> Popen[str]:
    binary = str(profile.cwd / profile.binary)
    kwargs: dict = {
        "cwd": profile.cwd,
        "env": proc_env,
    }
    if capture_stdout:
        kwargs["stdout"] = subprocess.PIPE
        kwargs["stderr"] = subprocess.STDOUT
    return subprocess.Popen([binary, "--host", host, "--port", str(port)], text=True, **kwargs)


@pytest.fixture(name="proc_env", scope="package")
def proc_env_fixture() -> Iterator[dict[str, str]]:
    yield {}


@pytest.fixture(name="build_artifacts", autouse=True, scope="package")
def build_artifacts_fixture() -> Iterator[bool]:
    subprocess.run(backend_profile.build_cmd, cwd=backend_profile.cwd).check_returncode()
    yield True


@pytest.fixture(name="raw_logged_server_daemon", scope="package")
def raw_logged_server_daemon_fixture(
    proc_env: dict[str, str],
    build_artifacts: bool,
) -> Iterator[tuple[Popen[str] | None, str]]:
    server_host, server_port = "127.0.0.1", 18080
    server_url = f"http://{server_host}:{server_port}"

    proc = _spawn_backend(backend_profile, proc_env, server_host, server_port, capture_stdout=True)

    assert proc.stdout
    os.set_blocking(proc.stdout.fileno(), False)

    wait_for_port(host=server_host, port=server_port)
    yield proc, server_url

    proc.send_signal(signal.SIGINT)
    assert proc.wait() == 0


@pytest.fixture(name="raw_server_daemon", scope="package")
def raw_server_daemon_fixture(
    proc_env: dict[str, str],
    build_artifacts: bool,
) -> Iterator[str]:
    server_host, server_port = "127.0.0.1", 18081
    server_url = f"http://{server_host}:{server_port}"

    proc = _spawn_backend(backend_profile, proc_env, server_host, server_port, capture_stdout=False)

    wait_for_port(host=server_host, port=server_port)
    yield server_url

    proc.send_signal(signal.SIGINT)
    assert proc.wait() == 0
