from abc import ABC, abstractmethod
from pathlib import Path


class DbReader(ABC):
    @abstractmethod
    async def dummy(self): ...


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
