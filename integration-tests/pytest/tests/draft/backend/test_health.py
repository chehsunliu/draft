from subprocess import Popen

import httpx

from draft_testkit.utils import read_all_json_logs


def test_health(strict_httpx_client_with_server_daemon: tuple[httpx.Client, Popen[str] | None]):
    client, server_daemon = strict_httpx_client_with_server_daemon
    r = client.get("/api/v1/health")

    assert r.status_code == 200, r.text
    assert r.json() == {"data": {"status": "ok"}}

    if not server_daemon:
        return

    logs = read_all_json_logs(server_daemon.stdout)
    assert len(logs) == 0


def test_unknown_path(raw_server_daemon: str):
    with httpx.Client(base_url=raw_server_daemon) as httpx_client:
        r = httpx_client.get("/api/v1/unknown")

    assert r.status_code == 404, r.text
    assert r.json() == {"error": {"message": "Not Found"}}


def test_method_not_allowed(raw_server_daemon: str):
    with httpx.Client(base_url=raw_server_daemon) as httpx_client:
        r = httpx_client.post("/api/v1/health")

    assert r.status_code == 405, r.text
    assert r.json() == {"error": {"message": "Method Not Allowed"}}
