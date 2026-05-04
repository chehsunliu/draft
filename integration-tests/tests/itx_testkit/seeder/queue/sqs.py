import json
from typing import Any

import aioboto3

from itx_testkit.seeder.queue.base import QUEUE_KEYS, QueueReader, QueueSeeder


class SqsQueueReader(QueueReader):
    def __init__(self, client, queue_url: str):
        self._client = client
        self._queue_url = queue_url

    async def receive_one(self, *, timeout_seconds: float = 5.0) -> dict[str, Any] | None:
        # AWS SDK long-poll waits up to `WaitTimeSeconds` (max 20). It expects an int.
        wait = max(1, min(20, int(timeout_seconds)))
        resp = await self._client.receive_message(
            QueueUrl=self._queue_url,
            WaitTimeSeconds=wait,
            MaxNumberOfMessages=1,
        )
        msgs = resp.get("Messages", [])
        if not msgs:
            return None
        msg = msgs[0]
        await self._client.delete_message(QueueUrl=self._queue_url, ReceiptHandle=msg["ReceiptHandle"])
        return json.loads(msg["Body"])


class SqsQueueSeeder(QueueSeeder):
    def __init__(self, *, endpoint_url: str, queue_urls: dict[str, str]):
        self._endpoint_url = endpoint_url
        self._queue_urls = queue_urls
        self._session = aioboto3.Session()
        self._client_ctx = None
        self._client = None

    async def __aenter__(self) -> "SqsQueueSeeder":
        if self._client is None:
            client_ctx = self._session.client(
                "sqs",
                endpoint_url=self._endpoint_url,
                region_name="us-east-1",
                aws_access_key_id="x",
                aws_secret_access_key="x",
            )
            self._client_ctx = client_ctx
            self._client = await client_ctx.__aenter__()
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb) -> None:
        # Keep the client alive across tests; the test session calls `close()` once at teardown.
        pass

    async def close(self) -> None:
        if self._client_ctx is not None:
            await self._client_ctx.__aexit__(None, None, None)
            self._client_ctx = None
            self._client = None

    async def reset(self) -> None:
        assert self._client is not None
        for url in self._queue_urls.values():
            # ElasticMQ supports purge_queue; ignore "queue empty" errors gracefully.
            try:
                await self._client.purge_queue(QueueUrl=url)
            except Exception:
                pass

    def reader(self, queue_key: str) -> QueueReader:
        if queue_key not in QUEUE_KEYS:
            raise ValueError(f"unknown queue key {queue_key!r}; expected one of {QUEUE_KEYS}")
        assert self._client is not None
        return SqsQueueReader(self._client, self._queue_urls[queue_key])

    async def publish(self, queue_key: str, body: str) -> None:
        if queue_key not in QUEUE_KEYS:
            raise ValueError(f"unknown queue key {queue_key!r}; expected one of {QUEUE_KEYS}")
        assert self._client is not None
        await self._client.send_message(QueueUrl=self._queue_urls[queue_key], MessageBody=body)
