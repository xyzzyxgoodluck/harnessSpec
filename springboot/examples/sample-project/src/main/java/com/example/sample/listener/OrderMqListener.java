package com.example.sample.listener;

import com.example.sample.cache.IdempotentStore;
import com.example.sample.common.CacheKeys;
import com.example.sample.common.TraceId;
import com.example.sample.config.MqDestinations;
import com.example.sample.dto.OrderPaidMessage;
import com.example.sample.service.OrderService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** 订单支付消息监听：只做"取消息 → 幂等 → 调 Service → 手动 ack/nack"适配（CODING_STANDARDS §8）。 */
@Component
public class OrderMqListener {

  private static final Logger log = LoggerFactory.getLogger(OrderMqListener.class);

  /** 单条消息的最大处理尝试次数（有限重试；耗尽后 nack(requeue=false) 经 DLX 进 DLQ）。 */
  static final int MAX_ATTEMPTS = 3;

  /** 重试间隔（毫秒），避免瞬时故障下紧循环。 */
  static final long RETRY_INTERVAL_MILLIS = 50L;

  private final OrderService orderService;
  private final IdempotentStore idempotentStore;

  /** 构造器注入。 */
  public OrderMqListener(OrderService orderService, IdempotentStore idempotentStore) {
    this.orderService = orderService;
    this.idempotentStore = idempotentStore;
  }

  /**
   * 消费订单支付消息：手动 ack；按幂等键丢弃重复投递；重试耗尽后 nack 进死信队列。
   *
   * @param payload 消息体（JSON 反序列化，见 RabbitConfig 的 Jackson2JsonMessageConverter）
   * @param message 原始消息（取 deliveryTag 手动 ack）
   * @param channel AMQP 通道
   * @throws IOException ack/nack 通道异常
   */
  @RabbitListener(queues = MqDestinations.ORDER_PAID_QUEUE)
  public void onOrderPaid(OrderPaidMessage payload, Message message, Channel channel)
      throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    String messageId = payload.messageId();
    if (!idempotentStore.register(CacheKeys.idemOrderPaid(messageId))) {
      log.info("重复投递，按幂等键丢弃并 ack messageId={}", messageId);
      channel.basicAck(deliveryTag, false);
      return;
    }
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        orderService.handleOrderPaid(payload.data().orderId());
        channel.basicAck(deliveryTag, false);
        return;
      } catch (RuntimeException ex) {
        // 记录 + 有限重试；耗尽后转译为"进死信"动作（CODING_STANDARDS §6：捕获后必须记录/转译/上抛）
        log.warn("订单支付消息处理失败 attempt={}/{} messageId={}", attempt, MAX_ATTEMPTS, messageId, ex);
        sleepQuietly();
      }
    }
    log.error("订单支付消息重试耗尽，转入死信 messageId={} dlq={}", messageId, MqDestinations.ORDER_PAID_DLQ);
    channel.basicNack(deliveryTag, false, false);
  }

  private void sleepQuietly() {
    try {
      Thread.sleep(RETRY_INTERVAL_MILLIS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn("重试等待被中断 traceId={}", TraceId.current());
    }
  }
}
