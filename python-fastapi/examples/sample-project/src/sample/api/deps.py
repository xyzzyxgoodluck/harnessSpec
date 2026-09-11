"""依赖装配：engine/session/cache/service 的唯一装配点（组装根）。

测试通过 app.dependency_overrides 覆盖 get_order_repository / get_cache，
因此单元测试与接口测试都不需要真实 PostgreSQL/Redis。
"""

from collections.abc import AsyncIterator
from functools import lru_cache
from typing import Annotated

from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from sample.core.cache import CacheProtocol, RedisCache
from sample.core.config import get_settings
from sample.db.base import create_engine, create_session_factory
from sample.repositories.order_repository import OrderRepository, OrderRepositoryProtocol
from sample.services.order_service import OrderService

_session_factory: async_sessionmaker[AsyncSession] | None = None


def get_session_factory() -> async_sessionmaker[AsyncSession]:
    """惰性创建：import 期不连接数据库，避免测试/工具链意外建连。"""
    global _session_factory
    if _session_factory is None:
        settings = get_settings()
        engine = create_engine(settings.database_url, echo=settings.sql_echo)
        _session_factory = create_session_factory(engine)
    return _session_factory


async def get_session() -> AsyncIterator[AsyncSession]:
    """一个请求一个会话一个事务：事务边界在此收口，service 不手工 commit。"""
    async with get_session_factory()() as session, session.begin():
        yield session


def get_order_repository(
    session: Annotated[AsyncSession, Depends(get_session)],
) -> OrderRepositoryProtocol:
    return OrderRepository(session)


@lru_cache
def _cache_singleton() -> RedisCache:
    """连接池进程内复用；客户端不主动建连，首次命令才连 Redis。"""
    return RedisCache(get_settings().redis_url)


def get_cache() -> CacheProtocol:
    return _cache_singleton()


def get_order_service(
    repository: Annotated[OrderRepositoryProtocol, Depends(get_order_repository)],
    cache: Annotated[CacheProtocol, Depends(get_cache)],
) -> OrderService:
    return OrderService(
        repository=repository,
        cache=cache,
        cache_ttl=get_settings().order_cache_ttl_seconds,
    )
