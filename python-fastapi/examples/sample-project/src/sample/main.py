"""应用工厂：中间件、异常处理、路由的唯一装配点（uvicorn 入口为 `sample.main:app`）。"""

from fastapi import FastAPI

from sample.api.routers import health, orders
from sample.core.config import get_settings
from sample.core.errors import register_exception_handlers
from sample.core.logging import TraceIdMiddleware, configure_logging


def create_app() -> FastAPI:
    settings = get_settings()
    configure_logging("DEBUG" if settings.debug else "INFO")
    app = FastAPI(
        title=settings.app_name,
        version="0.1.0",
        # 交互式文档仅在 debug 打开：生产不暴露 /docs 与 /openapi.json。
        docs_url="/docs" if settings.debug else None,
        redoc_url=None,
        openapi_url="/openapi.json" if settings.debug else None,
    )
    app.add_middleware(TraceIdMiddleware)
    register_exception_handlers(app)
    app.include_router(health.router)
    app.include_router(orders.router)
    return app


app = create_app()
