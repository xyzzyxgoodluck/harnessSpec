"""订单仓储：SQLAlchemy 2.0 风格（select() + 异步会话），方法前缀 select/insert/update/delete。"""

from typing import Protocol

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from sample.models.order import Order


class OrderRepositoryProtocol(Protocol):
    """服务层依赖的端口：单元测试用内存假实现替换，不连数据库。"""

    async def select_by_order_no(self, order_no: str) -> Order | None: ...

    async def insert(self, order: Order) -> Order: ...


class OrderRepository:
    """真实实现：只做数据访问，不做业务判断、不发消息、不删缓存。"""

    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def select_by_order_no(self, order_no: str) -> Order | None:
        stmt = select(Order).where(Order.order_no == order_no)
        result = await self._session.execute(stmt)
        return result.scalar_one_or_none()

    async def insert(self, order: Order) -> Order:
        self._session.add(order)
        await self._session.flush()
        return order
