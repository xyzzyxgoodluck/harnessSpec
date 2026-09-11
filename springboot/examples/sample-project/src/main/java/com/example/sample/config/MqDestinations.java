package com.example.sample.config;

/** RabbitMQ 目标常量（点分小写；交换机/队列/RoutingKey/死信登记于此，禁散落字符串）。 */
public final class MqDestinations {

  /** 服务名（作为交换机/队列命名前缀）。 */
  public static final String SVC = "sample";

  /** 业务交换机（topic）。 */
  public static final String ORDER_EXCHANGE = SVC + ".order.exchange";

  /** 订单支付事件 RoutingKey。 */
  public static final String ORDER_PAID_RK = "order.paid";

  /** 订单支付事件队列（消费者：OrderMqListener）。 */
  public static final String ORDER_PAID_QUEUE = SVC + ".order.paid.queue";

  /** 订单域死信交换机（topic）。 */
  public static final String ORDER_DLX = SVC + ".order.dlx";

  /** 订单支付死信 RoutingKey（队列死信时改写为该 key，避免与业务 key 混淆）。 */
  public static final String ORDER_PAID_DLQ_RK = "order.paid.dlq";

  /** 订单支付死信队列。 */
  public static final String ORDER_PAID_DLQ = SVC + ".order.paid.queue.dlq";

  private MqDestinations() {}
}
