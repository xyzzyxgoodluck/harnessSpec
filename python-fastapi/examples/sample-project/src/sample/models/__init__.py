"""models：SQLAlchemy 2.0 表映射（纯载体，禁业务方法，禁直接作出参）。"""

from sample.db.base import Base
from sample.models.order import Order, OrderStatus

__all__ = ["Base", "Order", "OrderStatus"]
