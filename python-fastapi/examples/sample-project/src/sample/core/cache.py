"""Redis 使用：key 单一来源 + 统一序列化；写库后删缓存，不"先更新缓存"。"""

from typing import Protocol

from redis.asyncio import Redis


class CacheKeys:
    """key 布局登记（全库唯一事实源，禁止魔法字符串散落）：

    order:detail:{order_no}      缓存，TTL 见 Settings.order_cache_ttl_seconds
    idem:order:create:{request_id}  幂等，TTL 24h
    lock:order:create:{order_no}    分布式锁，看门狗自动续期
    """

    ORDER_DETAIL = "order:detail:{order_no}"
    IDEM_ORDER_CREATE = "idem:order:create:{request_id}"
    LOCK_ORDER_CREATE = "lock:order:create:{order_no}"


def order_detail_key(order_no: str) -> str:
    return CacheKeys.ORDER_DETAIL.format(order_no=order_no)


class CacheProtocol(Protocol):
    """服务层依赖的缓存端口：单元测试用内存假实现替换，不连 Redis。"""

    async def get(self, key: str) -> str | None: ...

    async def set(self, key: str, value: str, ttl_seconds: int) -> None: ...

    async def delete(self, key: str) -> None: ...


class RedisCache:
    """Redis 适配器：客户端在基础设施层创建，业务层只依赖 CacheProtocol。"""

    def __init__(self, url: str) -> None:
        self._client: Redis = Redis.from_url(url, decode_responses=True)

    async def get(self, key: str) -> str | None:
        value = await self._client.get(key)
        return value if isinstance(value, str) else None

    async def set(self, key: str, value: str, ttl_seconds: int) -> None:
        await self._client.set(key, value, ex=ttl_seconds)

    async def delete(self, key: str) -> None:
        await self._client.delete(key)
