"""错误码单一来源 + 全局异常处理：业务错误抛 BizError，对外只给 code/message/traceId。

规则（详见 docs/CODING_STANDARDS.md §10）：
    编码分段 1xxxxx 系统 / 2xxxxx 参数 / 3xxxxx 业务 / 4xxxxx 认证授权 / 5xxxxx 外部依赖。
    命名 {域}_{对象}_{原因}，只增不删，新增前先查重。
"""

import logging
from enum import StrEnum

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from sample.core.logging import TRACE_ID_HEADER, current_trace_id

logger = logging.getLogger(__name__)


class ErrorCode(StrEnum):
    SYSTEM_ERROR = "100000"
    PARAM_INVALID = "200001"
    ORDER_NOT_FOUND = "300001"
    ORDER_NO_DUPLICATED = "300002"
    UNAUTHORIZED = "400001"
    FORBIDDEN = "400002"
    DEPENDENCY_UNAVAILABLE = "500001"


_MESSAGE: dict[ErrorCode, str] = {
    ErrorCode.SYSTEM_ERROR: "系统繁忙，请稍后重试",
    ErrorCode.PARAM_INVALID: "参数校验失败",
    ErrorCode.ORDER_NOT_FOUND: "订单不存在：{0}",
    ErrorCode.ORDER_NO_DUPLICATED: "订单号已存在：{0}",
    ErrorCode.UNAUTHORIZED: "未登录或登录已过期",
    ErrorCode.FORBIDDEN: "无权访问该资源",
    ErrorCode.DEPENDENCY_UNAVAILABLE: "依赖服务暂不可用，请稍后重试",
}

_HTTP_STATUS: dict[ErrorCode, int] = {
    ErrorCode.SYSTEM_ERROR: 500,
    ErrorCode.PARAM_INVALID: 422,
    ErrorCode.ORDER_NOT_FOUND: 404,
    ErrorCode.ORDER_NO_DUPLICATED: 409,
    ErrorCode.UNAUTHORIZED: 401,
    ErrorCode.FORBIDDEN: 403,
    ErrorCode.DEPENDENCY_UNAVAILABLE: 503,
}


class BizError(Exception):
    """业务错误载体：可预期、可向用户展示；系统错误不套用本类。"""

    def __init__(self, code: ErrorCode, *detail: object) -> None:
        super().__init__(code, *detail)
        self.code = code
        self.detail = detail

    @property
    def message(self) -> str:
        template = _MESSAGE[self.code]
        return template.format(*self.detail) if self.detail else template

    @property
    def http_status(self) -> int:
        return _HTTP_STATUS[self.code]


def _trace_of(request: Request) -> str:
    """优先取中间件写入的 traceId（系统错误在中间件外层被兜底时仍可取到）。"""
    trace_id = getattr(request.state, "trace_id", None)
    return trace_id if isinstance(trace_id, str) else current_trace_id()


def _error_body(code: ErrorCode, message: str, trace_id: str) -> dict[str, str]:
    return {"code": code.value, "message": message, "traceId": trace_id}


def _error_response(code: ErrorCode, message: str, trace_id: str) -> JSONResponse:
    """错误响应统一出口：body 与**响应头**都回带 traceId。

    为什么必须在这里补头：异常路径上 `TraceIdMiddleware` 的补头语句不可达
    （`call_next` 抛异常时不会走到响应头设置），必须由处理器自己回带。
    """
    return JSONResponse(
        status_code=_HTTP_STATUS[code],
        content=_error_body(code, message, trace_id),
        headers={TRACE_ID_HEADER: trace_id},
    )


def register_exception_handlers(app: FastAPI) -> None:
    """全局兜底：业务错误 WARN，系统错误 ERROR 且只记日志、不外泄堆栈。"""

    @app.exception_handler(BizError)
    async def _handle_biz_error(request: Request, exc: BizError) -> JSONResponse:
        trace_id = _trace_of(request)
        logger.warning(
            "biz_error code=%s traceId=%s path=%s", exc.code.value, trace_id, request.url.path
        )
        return _error_response(exc.code, exc.message, trace_id)

    @app.exception_handler(RequestValidationError)
    async def _handle_param_error(request: Request, exc: RequestValidationError) -> JSONResponse:
        trace_id = _trace_of(request)
        logger.info("param_invalid traceId=%s path=%s", trace_id, request.url.path)
        code = ErrorCode.PARAM_INVALID
        return _error_response(code, _MESSAGE[code], trace_id)

    @app.exception_handler(Exception)
    async def _handle_unexpected(request: Request, exc: Exception) -> JSONResponse:
        trace_id = _trace_of(request)
        logger.error("system_error traceId=%s path=%s", trace_id, request.url.path, exc_info=exc)
        code = ErrorCode.SYSTEM_ERROR
        return _error_response(code, _MESSAGE[code], trace_id)
