"""订单用例单元测试：正常路径 + 每个业务异常分支断言 ErrorCode + 副作用（删缓存）。"""

import pytest

from sample.core.cache import order_detail_key
from sample.core.errors import BizError, ErrorCode
from sample.models.order import Order
from sample.schemas.order import OrderCreateRequest
from sample.services.order_service import OrderService
from tests.fakes import FakeCache, FakeOrderRepository


def _service(repository: FakeOrderRepository, cache: FakeCache) -> OrderService:
    return OrderService(repository=repository, cache=cache, cache_ttl=600)


async def test_create_order_returns_created_order(
    fake_repository: FakeOrderRepository, fake_cache: FakeCache
) -> None:
    service = _service(fake_repository, fake_cache)

    result = await service.create_order(
        OrderCreateRequest(order_no="ORD-20260910-0001", amount_cents=9900)
    )

    assert result.order_no == "ORD-20260910-0001"
    assert result.status == "CREATED"
    assert result.amount_cents == 9900
    assert "ORD-20260910-0001" in fake_repository.orders


async def test_create_order_deletes_cache_after_write(
    fake_repository: FakeOrderRepository, fake_cache: FakeCache
) -> None:
    fake_cache.store[order_detail_key("ORD-20260910-0002")] = "stale"
    service = _service(fake_repository, fake_cache)

    await service.create_order(OrderCreateRequest(order_no="ORD-20260910-0002", amount_cents=100))

    assert fake_cache.deleted == [order_detail_key("ORD-20260910-0002")]
    assert fake_cache.store == {}


async def test_create_duplicated_order_raises_biz_error(
    fake_repository: FakeOrderRepository, fake_cache: FakeCache
) -> None:
    fake_repository.orders["ORD-20260910-0003"] = Order(
        order_no="ORD-20260910-0003", amount_cents=1
    )
    service = _service(fake_repository, fake_cache)

    with pytest.raises(BizError) as exc_info:
        await service.create_order(
            OrderCreateRequest(order_no="ORD-20260910-0003", amount_cents=100)
        )

    assert exc_info.value.code is ErrorCode.ORDER_NO_DUPLICATED
    assert exc_info.value.http_status == 409


async def test_get_missing_order_raises_biz_error(
    fake_repository: FakeOrderRepository, fake_cache: FakeCache
) -> None:
    service = _service(fake_repository, fake_cache)

    with pytest.raises(BizError) as exc_info:
        await service.get_order("ORD-20260910-9999")

    assert exc_info.value.code is ErrorCode.ORDER_NOT_FOUND
    assert exc_info.value.http_status == 404
    assert "ORD-20260910-9999" in exc_info.value.message
