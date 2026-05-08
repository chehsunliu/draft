import os
import subprocess
from pathlib import Path
from subprocess import Popen
from typing import NamedTuple


class ArtifactProfile(NamedTuple):
    cwd: Path
    build_cmd: list[str]
    backend_cmd: list[str]
    control_worker_cmd: list[str] | None = None

    def build(self) -> None:
        subprocess.run(self.build_cmd, cwd=self.cwd).check_returncode()

    def spawn_backend(
        self,
        proc_env: dict[str, str],
        host: str,
        port: int,
        *,
        capture_stdout: bool,
    ) -> Popen[str]:
        kwargs: dict = {
            "cwd": self.cwd,
            "env": {**os.environ, **proc_env},
        }
        if capture_stdout:
            kwargs["stdout"] = subprocess.PIPE
            kwargs["stderr"] = subprocess.STDOUT
        return subprocess.Popen(
            [*self.backend_cmd, "--host", host, "--port", str(port)],
            text=True,
            **kwargs,
        )

    def spawn_control_worker(
        self,
        proc_env: dict[str, str],
        *,
        capture_stdout: bool,
    ) -> Popen[str]:
        if self.control_worker_cmd is None:
            raise RuntimeError("control_worker_cmd not configured for this profile")
        kwargs: dict = {
            "cwd": self.cwd,
            "env": {**os.environ, **proc_env},
        }
        if capture_stdout:
            kwargs["stdout"] = subprocess.PIPE
            kwargs["stderr"] = subprocess.STDOUT
        return subprocess.Popen(self.control_worker_cmd, text=True, **kwargs)
