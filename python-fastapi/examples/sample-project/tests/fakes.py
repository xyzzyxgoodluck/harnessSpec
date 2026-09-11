"""测试用内存假实现：满足仓储/缓存 Protocol，单元与接口测试不连真实中间件。"""

from collections.abc import Sequence

from sample.models.order import Order, OrderStatus


class FakeOrderRepository:
    """内存仓储：行为对齐 OrderRepository（含 flush 时应用列默认值的语义）。"""

    def __init__(self, orders: Sequence[Order] | None = None) -> None:
        self.orders: dict[str, Order] = {order.order_no: order for order in orders or []}

    async def select_by_order_no(self, order_no: str) -> Order | None:
        return self.orders.get(order_no)

    async def insert(self, order: Order) -> Order:
        if order.status is None:
            # 等价于真实实现中 flush 时应用列默认值 default=OrderStatus.CREATED。
            order.status = OrderStatus.CREATED
        self.orders[order.order_no] = order
        return order


class FakeCache:
    """内存缓存：记录删除动作，便于断言"写库后删缓存"。"""

    def __init__(self) -> None:
        self.store: dict[str, str] = {}
        self.deleted: list[str] = []

    async def get(self, key: str) -> str | None:
        return self.store.get(key)

    async def set(self, key: str, value: str, ttl_seconds: int) -> None:
        self.store[key] = value

    async def delete(self, key: str) -> None:
        self.deleted.append(key)
        self.store.pop(key, None)


class FailingOrderRepository:
    """故障注入：验证系统错误被兜底为 500 且不外泄堆栈。"""

    async def select_by_order_no(self, order_no: str) -> Order | None:
        raise RuntimeError(f"database unavailable for {order_no}")

    async def insert(self, order: Order) -> Order:
        raise RuntimeError("database unavailable")
