from typing import Any, Mapping

import httpx
from openapi_core import OpenAPI
from openapi_core.datatypes import RequestParameters
from openapi_core.protocols import Request as OpenAPIRequest
from openapi_core.protocols import Response as OpenAPIResponse
from werkzeug.datastructures import ImmutableMultiDict


class HttpxOpenAPIRequest(OpenAPIRequest):
    def __init__(self, request: httpx.Request):
        self.req = request
        # Use a MultiDict so openapi-core treats array-typed query params
        # (e.g. ?status=completed&status=error) as iterables of values.
        # A plain dict(...) would collapse repeats to the last value and
        # fail array validation.
        self.parameters = RequestParameters(
            query=ImmutableMultiDict(request.url.params.multi_items()),
            header=request.headers,
            cookie={},
        )

    @property
    def method(self) -> str:
        return self.req.method.lower()

    @property
    def body(self) -> bytes | None:
        return self.req.content

    @property
    def content_type(self) -> str:
        return self.req.headers.get("content-type", "").lower()

    @property
    def host_url(self) -> str:
        return f"{self.req.url.scheme}://{self.req.url.netloc.decode()}"

    @property
    def path(self) -> str:
        return self.req.url.path


class HttpxOpenAPIResponse(OpenAPIResponse):
    def __init__(self, response: httpx.Response, data: bytes | None):
        self.resp = response
        self._data = data

    @property
    def status_code(self) -> int:
        return self.resp.status_code

    @property
    def content_type(self) -> str:
        return self.resp.headers.get("content-type", "").lower()

    @property
    def headers(self) -> Mapping[str, Any]:
        return self.resp.headers

    @property
    def data(self) -> bytes | None:
        return self._data


class OpenAPIValidator:
    def __init__(self, schema: dict, *, validate_request: bool = True, validate_response: bool = True):
        self._openapi = OpenAPI.from_dict(schema)
        self._validate_request = validate_request
        self._validate_response = validate_response

    def validate_request(self, request: httpx.Request):
        if not self._validate_request:
            return

        self._openapi.validate_request(HttpxOpenAPIRequest(request))

    def validate_response(self, response: httpx.Response):
        if not self._validate_response:
            return

        data = response.read()
        self._openapi.validate_response(
            HttpxOpenAPIRequest(response.request),
            HttpxOpenAPIResponse(response, data),
        )

    async def validate_async_request(self, request: httpx.Request):
        if not self._validate_request:
            return

        self._openapi.validate_request(HttpxOpenAPIRequest(request))

    async def validate_async_response(self, response: httpx.Response):
        if not self._validate_response:
            return

        data = await response.aread()
        self._openapi.validate_response(
            HttpxOpenAPIRequest(response.request),
            HttpxOpenAPIResponse(response, data),
        )

    @property
    def as_event_hooks(self):
        return {"request": [self.validate_request], "response": [self.validate_response]}

    @property
    def as_async_event_hooks(self):
        return {"request": [self.validate_async_request], "response": [self.validate_async_response]}
