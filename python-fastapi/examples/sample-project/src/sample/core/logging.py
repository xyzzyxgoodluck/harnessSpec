"""日志与链路追踪：traceId 经 ContextVar 贯穿 HTTP 请求、日志与消息体。"""

import logging
import uuid
from contextvars import ContextVar
from typing import Any

from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.requests import Request
from starlette.responses import Response

TRACE_ID_HEADER = "X-Trace-Id"
LOG_FORMAT = "%(asctime)s %(levelname)s %(name)s traceId=%(trace_id)s %(message)s"

_trace_id: ContextVar[str] = ContextVar("trace_id", default="-")


class _TraceIdFilter(logging.Filter):
    """把当前上下文 traceId 注入每条日志记录，保证日志与响应同源。"""

    def filter(self, record: logging.LogRecord) -> bool:
        record.__dict__["trace_id"] = _trace_id.get()
        return True


def configure_logging(level: str = "INFO") -> None:
    """统一日志出口与格式；禁止字符串拼接打日志，一律参数化。"""
    handler = logging.StreamHandler()
    handler.setFormatter(logging.Formatter(LOG_FORMAT))
    handler.addFilter(_TraceIdFilter())
    root = logging.getLogger()
    root.handlers = [handler]
    root.setLevel(level)


def current_trace_id() -> str:
    return _trace_id.get()


class TraceIdMiddleware(BaseHTTPMiddleware):
    """入口生成/透传 traceId；响应头回带，便于客户端与日志对账。"""

    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        trace_id = request.headers.get(TRACE_ID_HEADER) or uuid.uuid4().hex
        token = _trace_id.set(trace_id)
        request.state.trace_id = trace_id
        try:
            response = await call_next(request)
        finally:
            _trace_id.reset(token)
        response.headers[TRACE_ID_HEADER] = trace_id
        return response


def log_extra(**fields: Any) -> dict[str, Any]:
    """结构化附加字段的占位入口：团队接入 JSON 日志时在此统一扩展。"""
    return fields
