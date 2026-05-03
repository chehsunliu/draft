from abc import ABC, abstractmethod
from pathlib import Path
from typing import Any


class DbReader(ABC):
    @abstractmethod
    async def get_post(self, post_id: int, user_id: str) -> dict[str, Any]: ...

    @abstractmethod
    async def is_subscribed(self, subscriber_id: str, author_id: str) -> bool: ...


class DbSeeder(ABC):
    @abstractmethod
    async def __aenter__(self) -> "DbSeeder": ...

    @abstractmethod
    async def __aexit__(self, exc_type, exc_val, exc_tb) -> None: ...

    @abstractmethod
    async def reset_tables(self): ...

    @abstractmethod
    async def write_data(self, folder_path: Path): ...

    @abstractmethod
    def reader(self) -> DbReader: ...
