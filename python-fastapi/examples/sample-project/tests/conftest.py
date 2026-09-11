"""测试夹具：依赖覆盖（dependency_overrides）是接口测试不连中间件唯一手段。"""

from collections.abc import AsyncIterator

import pytest
from fastapi import FastAPI
from httpx import ASGITransport, AsyncClient

from sample.api.deps import get_cache, get_order_repository
from sample.main import create_app
from tests.fakes import FakeCache, FakeOrderRepository


@pytest.fixture
def fake_repository() -> FakeOrderRepository:
    return FakeOrderRepository()


@pytest.fixture
def fake_cache() -> FakeCache:
    return FakeCache()


@pytest.fixture
def app(fake_repository: FakeOrderRepository, fake_cache: FakeCache) -> FastAPI:
    application = create_app()
    application.dependency_overrides[get_order_repository] = lambda: fake_repository
    application.dependency_overrides[get_cache] = lambda: fake_cache
    return application


@pytest.fixture
async def client(app: FastAPI) -> AsyncIterator[AsyncClient]:
    async with AsyncClient(
        transport=ASGITransport(app=app), base_url="http://testserver"
    ) as async_client:
        yield async_client
