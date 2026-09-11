"""services：业务规则与事务边界的唯一所在（不感知 HTTP，不拼 SQL）。"""

from sample.services.order_service import OrderService

__all__ = ["OrderService"]
