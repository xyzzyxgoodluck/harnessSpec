"""RabbitMQ 拓扑与消息信封单一来源（声明集中于此，禁止散落字符串）。"""

import uuid
from datetime import UTC, datetime
from typing import Any


class MqTopology:
    """拓扑登记：交换机/RoutingKey/队列/死信，变更即架构变更（记 ADR）。"""

    ORDER_EXCHANGE = "sample.order.exchange"
    ORDER_DLX = "sample.order.dlx"
    ORDER_CREATED_ROUTING_KEY = "order.created"
    ORDER_CREATED_QUEUE = "sample.order.created.queue"
    ORDER_CREATED_DLQ = "sample.order.created.queue.dlq"


def build_envelope(event_type: str, data: dict[str, Any], trace_id: str) -> dict[str, Any]:
    """统一信封：messageId 用于消费幂等，traceId 用于全链路追踪。"""
    return {
        "messageId": uuid.uuid4().hex,
        "traceId": trace_id,
        "type": event_type,
        "timestamp": datetime.now(UTC).isoformat(),
        "data": data,
    }
