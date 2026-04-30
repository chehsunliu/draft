import os
from pathlib import Path
from typing import Iterator

import pytest

from itx_testkit.profile import ArtifactProfile

REPO_ROOT = Path(__file__).parent / "../../../.."

profiles: dict[str, ArtifactProfile] = {
    "rust": ArtifactProfile(
        cwd=REPO_ROOT / "itx-rs",
        build_cmd=["cargo", "build"],
        backend_binary="target/debug/itx-backend",
    ),
    "golang": ArtifactProfile(
        cwd=REPO_ROOT / "itx-go",
        build_cmd=["make", "build"],
        backend_binary="bin/itx-backend",
    ),
}

itx_lang = os.environ.get("ITX_LANG", "rust")
if itx_lang not in profiles:
    raise ValueError(f"ITX_LANG must be one of {sorted(profiles)}; got {itx_lang!r}")

profile = profiles[itx_lang]


@pytest.fixture(name="artifact_profile", autouse=True, scope="package")
def artifact_profile_fixture() -> Iterator[ArtifactProfile]:
    profile.build()
    yield profile
