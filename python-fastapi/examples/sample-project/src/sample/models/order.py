"""订单表映射：主键策略、状态枚举、审计字段全库统一。"""

from enum import StrEnum

from sqlalchemy import BigInteger, Integer, String
from sqlalchemy import Enum as SAEnum
from sqlalchemy.orm import Mapped, mapped_column

from sample.db.base import AuditMixin, Base


class OrderStatus(StrEnum):
    """稳定状态机用枚举，不落字典表（可运营可配置项才落字典表）。"""

    CREATED = "CREATED"
    PAID = "PAID"
    CANCELLED = "CANCELLED"


class Order(Base, AuditMixin):
    __tablename__ = "t_order"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    order_no: Mapped[str] = mapped_column(String(32), unique=True, index=True)
    status: Mapped[OrderStatus] = mapped_column(
        SAEnum(OrderStatus, name="order_status"), default=OrderStatus.CREATED, nullable=False
    )
    amount_cents: Mapped[int] = mapped_column(Integer, nullable=False)
