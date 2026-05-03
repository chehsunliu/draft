import asyncio
import json
from typing import Any

import aio_pika

from itx_testkit.seeder.queue.base import QUEUE_KEYS, QueueReader, QueueSeeder


class RabbitQueueReader(QueueReader):
    def __init__(self, channel: aio_pika.abc.AbstractChannel, queue_name: str):
        self._channel = channel
        self._queue_name = queue_name

    async def receive_one(self, *, timeout_seconds: float = 5.0) -> dict[str, Any] | None:
        queue = await self._channel.get_queue(self._queue_name, ensure=False)
        deadline = asyncio.get_event_loop().time() + timeout_seconds
        # `queue.get` is an immediate poll; loop with a short sleep to simulate the long-poll
        # semantics we expose in QueueReader.
        while asyncio.get_event_loop().time() < deadline:
            try:
                msg = await queue.get(no_ack=False, fail=True)
            except aio_pika.exceptions.QueueEmpty:
                await asyncio.sleep(0.05)
                continue
            await msg.ack()
            return json.loads(msg.body.decode("utf-8"))
        return None


class RabbitQueueSeeder(QueueSeeder):
    def __init__(self, *, url: str, queue_names: dict[str, str]):
        self._url = url
        self._queue_names = queue_names
        self._connection: aio_pika.abc.AbstractRobustConnection | None = None
        self._channel: aio_pika.abc.AbstractChannel | None = None

    async def __aenter__(self) -> "RabbitQueueSeeder":
        if self._connection is None:
            self._connection = await aio_pika.connect_robust(self._url)
            self._channel = await self._connection.channel()
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb) -> None:
        # Keep the connection across tests for speed; the test session calls `close()` once at teardown.
        pass

    async def close(self) -> None:
        if self._connection is not None:
            await self._connection.close()
            self._connection = None
            self._channel = None

    async def reset(self) -> None:
        assert self._channel is not None
        for name in self._queue_names.values():
            queue = await self._channel.get_queue(name, ensure=False)
            await queue.purge()

    def reader(self, queue_key: str) -> QueueReader:
        if queue_key not in QUEUE_KEYS:
            raise ValueError(f"unknown queue key {queue_key!r}; expected one of {QUEUE_KEYS}")
        assert self._channel is not None
        return RabbitQueueReader(self._channel, self._queue_names[queue_key])
