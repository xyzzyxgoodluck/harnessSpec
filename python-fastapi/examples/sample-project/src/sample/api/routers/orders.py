"""订单接口：只做 HTTP 适配（校验由 schema 承担、业务由 service 承担）。"""

from typing import Annotated

from fastapi import APIRouter, Depends, status

from sample.api.deps import get_order_service
from sample.schemas.common import ErrorResponse
from sample.schemas.order import OrderCreateRequest, OrderResponse
from sample.services.order_service import OrderService

router = APIRouter(prefix="/api/v1/orders", tags=["order"])

ServiceDep = Annotated[OrderService, Depends(get_order_service)]


@router.post(
    "",
    response_model=OrderResponse,
    status_code=status.HTTP_201_CREATED,
    summary="创建订单",
    responses={409: {"model": ErrorResponse, "description": "订单号已存在"}},
)
async def create_order(payload: OrderCreateRequest, service: ServiceDep) -> OrderResponse:
    return await service.create_order(payload)


@router.get(
    "/{order_no}",
    response_model=OrderResponse,
    summary="按订单号查询订单",
    responses={404: {"model": ErrorResponse, "description": "订单不存在"}},
)
async def get_order(order_no: str, service: ServiceDep) -> OrderResponse:
    return await service.get_order(order_no)
