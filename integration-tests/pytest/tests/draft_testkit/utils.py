import json
import socket
import time
from typing import IO, Any, AnyStr

RFC3339_RE = r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}"


def wait_for_port(host: str, port: int):
    timeout, interval = 5.0, 0.1
    deadline = time.time() + timeout
    while time.time() < deadline:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.settimeout(interval)
            try:
                sock.connect((host, port))
                return
            except socket.timeout, ConnectionRefusedError:
                time.sleep(interval)

    raise Exception(f"{host}:{port} is not available")


def read_all_logs(io: IO[AnyStr] | None) -> list[AnyStr]:
    if io is None:
        return []

    return [log for log in io.readlines()]


def read_all_json_logs(io: IO[AnyStr] | None) -> list[dict[str, Any]]:
    if io is None:
        return []

    logs: list[dict[str, Any]] = []
    for log in io.readlines():
        try:
            logs.append(json.loads(log))
        except json.JSONDecodeError:
            pass
    return logs
