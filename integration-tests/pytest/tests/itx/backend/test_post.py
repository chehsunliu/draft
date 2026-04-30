import httpx


def test_list_posts(strict_httpx_client: httpx.Client):
    headers = {"X-Itx-User-Id": "11111111-1111-1111-1111-111111111111"}

    r = strict_httpx_client.get("/api/v1/posts", headers=headers)

    assert r.status_code == 200, r.text
    assert r.json() == {"data": {"items": []}}
