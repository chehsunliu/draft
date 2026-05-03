from abc import ABC, abstractmethod
from typing import Any

# Logical queue keys, decoupled from the broker-specific name (e.g. "test-itx-control-standard").
QUEUE_KEYS = ["control_standard", "control_premium", "compute_standard", "compute_premium"]


class QueueReader(ABC):
    @abstractmethod
    async def receive_one(self, *, timeout_seconds: float = 5.0) -> dict[str, Any] | None:
        """Wait up to `timeout_seconds` for one message, ack it, and return its parsed JSON body.
        Returns None if no message arrived within the timeout."""


class QueueSeeder(ABC):
    @abstractmethod
    async def __aenter__(self) -> "QueueSeeder": ...

    @abstractmethod
    async def __aexit__(self, exc_type, exc_val, exc_tb) -> None: ...

    @abstractmethod
    async def reset(self) -> None:
        """Purge every queue so each test starts with empty queues."""

    @abstractmethod
    def reader(self, queue_key: str) -> QueueReader:
        """Return a reader bound to `queue_key` (one of QUEUE_KEYS)."""
