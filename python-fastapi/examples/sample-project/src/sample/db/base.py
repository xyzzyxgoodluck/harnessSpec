"""数据库基础设施：DeclarativeBase、审计字段 Mixin、异步会话工厂。"""

from datetime import datetime

from sqlalchemy import DateTime, func
from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    """全库统一的 declarative base；metadata 是 Alembic autogenerate 的唯一输入。"""


class AuditMixin:
    """审计字段统一命名 `created_at`/`updated_at`（见 docs/ARCHITECTURE.md §5、§7）。

    - `created_at`：插入时由**数据库默认值**填充（`server_default`）。
    - `updated_at`：插入默认值 + **ORM 侧** `onupdate` 刷新（走 ORM 的更新会刷新该列；
      若存在绕过 ORM 的裸 SQL 更新，需另行用触发器维护——本项目不启用，故不为它建触发器）。
    """

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )


def create_engine(database_url: str, *, echo: bool = False) -> AsyncEngine:
    return create_async_engine(database_url, echo=echo, pool_pre_ping=True)


def create_session_factory(engine: AsyncEngine) -> async_sessionmaker[AsyncSession]:
    return async_sessionmaker(engine, expire_on_commit=False)
