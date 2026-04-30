import os
from pathlib import Path
from typing import Iterator

import pytest

from draft_testkit.profile import ArtifactProfile

REPO_ROOT = Path(__file__).parent / "../../../.."

profiles: dict[str, ArtifactProfile] = {
    "rust": ArtifactProfile(
        cwd=REPO_ROOT / "draft-rs",
        build_cmd=["cargo", "build"],
        backend_binary="target/debug/draft-backend",
    ),
    "golang": ArtifactProfile(
        cwd=REPO_ROOT / "draft-go",
        build_cmd=["make", "build"],
        backend_binary="bin/draft-backend",
    ),
}

draft_lang = os.environ.get("DRAFT_LANG", "rust")
if draft_lang not in profiles:
    raise ValueError(f"DRAFT_LANG must be one of {sorted(profiles)}; got {draft_lang!r}")

profile = profiles[draft_lang]


@pytest.fixture(name="artifact_profile", autouse=True, scope="package")
def artifact_profile_fixture() -> Iterator[ArtifactProfile]:
    profile.build()
    yield profile
