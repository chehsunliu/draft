import httpx
import pytest

UNAUTHORIZED_BODY = {"error": {"message": "Unauthorized"}}

placeholder_id = "00000000-0000-0000-0000-000000000000"


class TestUnauthorized:
    """All protected endpoints must return 401 when no tenant ID is provided."""

    @pytest.mark.parametrize(
        "method, path",
        [
            ("GET", "/api/v1/posts"),
            ("POST", "/api/v1/posts"),
            ("GET", "/api/v1/posts/1"),
            ("PATCH", "/api/v1/posts/1"),
            ("DELETE", "/api/v1/posts/1"),
            ("GET", "/api/v1/users/me"),
            ("GET", f"/api/v1/users/{placeholder_id}/subscriptions"),
            ("PUT", f"/api/v1/subscriptions/{placeholder_id}"),
            ("DELETE", f"/api/v1/subscriptions/{placeholder_id}"),
        ],
    )
    async def test_returns_401_without_tenant_id(self, lenient_httpx_client: httpx.Client, method: str, path: str):
        r = lenient_httpx_client.request(method, path)

        assert r.status_code == 401, r.text
        assert r.json() == UNAUTHORIZED_BODY
