"""订单用例编排：业务规则、错误分类、写库后删缓存。"""

import logging

from sample.core.cache import CacheProtocol, order_detail_key
from sample.core.errors import BizError, ErrorCode
from sample.models.order import Order
from sample.repositories.order_repository import OrderRepositoryProtocol
from sample.schemas.order import OrderCreateRequest, OrderResponse

logger = logging.getLogger(__name__)


class OrderService:
    def __init__(
        self, repository: OrderRepositoryProtocol, cache: CacheProtocol, cache_ttl: int = 600
    ) -> None:
        self._repository = repository
        self._cache = cache
        self._cache_ttl = cache_ttl

    async def create_order(self, request: OrderCreateRequest) -> OrderResponse:
        """幂等前提：order_no 唯一索引兜底；重复创建抛业务错误（409）。"""
        existing = await self._repository.select_by_order_no(request.order_no)
        if existing is not None:
            raise BizError(ErrorCode.ORDER_NO_DUPLICATED, request.order_no)

        order = await self._repository.insert(
            Order(order_no=request.order_no, amount_cents=request.amount_cents)
        )
        # 写库后删缓存（不"先更新缓存"）；缓存 key 由 core/cache.py 单一提供。
        await self._cache.delete(order_detail_key(order.order_no))
        logger.info("order_created order_no=%s", order.order_no)
        return OrderResponse.model_validate(order)

    async def get_order(self, order_no: str) -> OrderResponse:
        order = await self._repository.select_by_order_no(order_no)
        if order is None:
            raise BizError(ErrorCode.ORDER_NOT_FOUND, order_no)
        return OrderResponse.model_validate(order)
