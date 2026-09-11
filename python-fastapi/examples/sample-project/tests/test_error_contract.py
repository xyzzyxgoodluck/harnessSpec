"""错误契约测试：错误响应结构稳定、系统错误不外泄堆栈、无堆栈/表名泄漏。"""

import re
from collections.abc import Iterator

import pytest
from fastapi import FastAPI
from httpx import ASGITransport, AsyncClient

from sample.api.deps import get_cache, get_order_repository
from sample.core.errors import BizError, ErrorCode
from sample.core.logging import TRACE_ID_HEADER
from sample.main import create_app
from sample.schemas.common import ErrorResponse
from tests.fakes import FailingOrderRepository, FakeCache

_ERROR_CODES = {code.value for code in ErrorCode}
_NAMING_RE = re.compile(r"^[A-Z][A-Z0-9_]*$")

# 错误码快照：对应 §10「只增不删、新增前先查重」。新增成员时同步更新本快照并说明原因。
_FROZEN_CODES = {
    "SYSTEM_ERROR": "100000",
    "PARAM_INVALID": "200001",
    "ORDER_NOT_FOUND": "300001",
    "ORDER_NO_DUPLICATED": "300002",
    "UNAUTHORIZED": "400001",
    "FORBIDDEN": "400002",
    "DEPENDENCY_UNAVAILABLE": "500001",
}


@pytest.fixture
def failing_client(fake_cache: FakeCache) -> Iterator[AsyncClient]:
    application: FastAPI = create_app()
    application.dependency_overrides[get_order_repository] = FailingOrderRepository
    application.dependency_overrides[get_cache] = lambda: fake_cache
    # raise_app_exceptions=False：Starlette 的 ServerErrorMiddleware 在返回兜底响应后仍会重抛异常，
    # 测试要断言的是"兜底响应本身"，故关闭 httpx 的重抛。
    transport = ASGITransport(app=application, raise_app_exceptions=False)
    yield AsyncClient(transport=transport, base_url="http://testserver")


async def test_error_codes_are_unique_and_grouped() -> None:
    assert len(_ERROR_CODES) == len(list(ErrorCode))
    assert all(code.isdigit() and len(code) == 6 for code in _ERROR_CODES)
    # 6 位分段：首位=大类，必须落在 1..5（系统 / 参数 / 业务 / 认证授权 / 外部依赖）
    assert all(code[0] in "12345" for code in _ERROR_CODES), "错误码首位必须在 1..5 分段内"
    # 命名 {域}_{对象}_{原因}：大写字母开头、仅大写与下划线
    assert all(_NAMING_RE.match(member.name) for member in ErrorCode), "错误码命名不符规范"


async def test_error_codes_are_frozen() -> None:
    """只增不删：删除成员或改值即失败；新增成员需同步更新快照（§10）。"""
    current = {member.name: member.value for member in ErrorCode}
    missing = sorted(set(_FROZEN_CODES) - set(current))
    changed = {n: (v, current[n]) for n, v in _FROZEN_CODES.items() if current.get(n, v) != v}
    assert not missing, f"错误码被删除（§10 只增不删）：{missing}"
    assert not changed, f"错误码取值被修改（语义变化应新增码）：{changed}"


async def test_biz_error_message_renders_detail() -> None:
    error = BizError(ErrorCode.ORDER_NOT_FOUND, "ORD-1")

    assert error.message == "订单不存在：ORD-1"
    assert error.http_status == 404


@pytest.mark.asyncio
async def test_error_paths_return_trace_id_in_body_and_header(client: AsyncClient) -> None:
    """错误路径的 traceId 必须同时出现在 body 与**响应头**（§11）。

    回归背景：500 兜底由异常处理器写出，中间件的补头语句在该路径不可达——
    故要求"错误分支也必须回带响应头"，防止只断言 200/201 造成假绿。
    """
    not_found = await client.get("/api/v1/orders/ORD-20260910-9999")
    assert not_found.status_code == 404

    invalid = await client.post("/api/v1/orders", json={"order_no": "", "amount_cents": -1})
    assert invalid.status_code == 422

    for response in (not_found, invalid):
        body = ErrorResponse.model_validate(response.json())
        assert body.traceId
        assert response.headers.get(TRACE_ID_HEADER) == body.traceId


@pytest.mark.asyncio
async def test_system_error_returns_500_without_stack_trace(failing_client: AsyncClient) -> None:
    async with failing_client as client:
        response = await client.get("/api/v1/orders/ORD-20260910-9999")

    assert response.status_code == 500
    body = ErrorResponse.model_validate(response.json())
    assert body.code == ErrorCode.SYSTEM_ERROR.value
    assert body.traceId
    # 500 兜底路径同样必须回带响应头（防止"只在 200 断言"造成假绿）
    assert response.headers.get(TRACE_ID_HEADER) == body.traceId
    raw = response.text
    assert "Traceback" not in raw
    assert "RuntimeError" not in raw
    assert "t_order" not in raw
