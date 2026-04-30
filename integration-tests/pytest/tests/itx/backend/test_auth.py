import httpx
import pytest

UNAUTHORIZED_BODY = {"error": {"message": "Unauthorized"}}


class TestUnauthorized:
    """All protected endpoints must return 401 when no tenant ID is provided."""

    @pytest.mark.parametrize(
        "method, path",
        [
            ("GET", "/api/v1/posts"),
        ],
    )
    async def test_returns_401_without_tenant_id(self, lenient_httpx_client: httpx.Client, method: str, path: str):
        r = lenient_httpx_client.request(method, path)

        assert r.status_code == 401, r.text
        assert r.json() == UNAUTHORIZED_BODY
