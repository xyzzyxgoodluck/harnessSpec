package com.example.sample.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sample.cache.IdempotentStore;
import com.example.sample.common.CacheKeys;
import com.example.sample.common.ErrorCode;
import com.example.sample.dto.OrderPaidMessage;
import com.example.sample.service.OrderService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/** MQ 监听单测：手动 ack、幂等去重、有限重试耗尽进死信（TMQ 不需要真实 RabbitMQ）。 */
@ExtendWith(MockitoExtension.class)
class OrderMqListenerTest {

  private static final long DELIVERY_TAG = 7L;
  private static final String MESSAGE_ID = "msg-1";

  @Mock private OrderService orderService;
  @Mock private IdempotentStore idempotentStore;
  @Mock private Channel channel;

  private OrderMqListener listener;

  @BeforeEach
  void setUp() {
    listener = new OrderMqListener(orderService, idempotentStore);
  }

  @Test
  void shouldAckWhenProcessedSuccessfully() throws IOException {
    when(idempotentStore.register(CacheKeys.idemOrderPaid(MESSAGE_ID))).thenReturn(true);

    listener.onOrderPaid(paidMessage(), rawMessage(DELIVERY_TAG), channel);

    verify(orderService).handleOrderPaid(42L);
    verify(channel).basicAck(DELIVERY_TAG, false);
    verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
  }

  @Test
  void shouldDiscardDuplicateDeliveryByAck() throws IOException {
    when(idempotentStore.register(CacheKeys.idemOrderPaid(MESSAGE_ID))).thenReturn(false);

    listener.onOrderPaid(paidMessage(), rawMessage(DELIVERY_TAG), channel);

    verify(orderService, never()).handleOrderPaid(any());
    verify(channel).basicAck(DELIVERY_TAG, false);
  }

  @Test
  void shouldRetryLimitedTimesThenNackToDeadLetterQueue() throws IOException {
    when(idempotentStore.register(CacheKeys.idemOrderPaid(MESSAGE_ID))).thenReturn(true);
    when(orderService.handleOrderPaid(42L)).thenThrow(ErrorCode.ORDER_NOT_FOUND.exception(42L));

    listener.onOrderPaid(paidMessage(), rawMessage(DELIVERY_TAG), channel);

    verify(orderService, times(OrderMqListener.MAX_ATTEMPTS)).handleOrderPaid(42L);
    verify(channel).basicNack(DELIVERY_TAG, false, false);
    verify(channel, never()).basicAck(anyLong(), anyBoolean());
  }

  @Test
  void shouldStopRetryingAfterSuccessOnSecondAttempt() throws IOException {
    when(idempotentStore.register(CacheKeys.idemOrderPaid(MESSAGE_ID))).thenReturn(true);
    when(orderService.handleOrderPaid(42L))
        .thenThrow(ErrorCode.ORDER_NOT_FOUND.exception(42L))
        .thenReturn(null);

    listener.onOrderPaid(paidMessage(), rawMessage(DELIVERY_TAG), channel);

    verify(orderService, times(2)).handleOrderPaid(42L);
    verify(channel).basicAck(DELIVERY_TAG, false);
  }

  @Test
  void shouldKeepIdempotentKeyBoundToMessageId() {
    assertThat(CacheKeys.idemOrderPaid(MESSAGE_ID)).isEqualTo("idem:order:paid:" + MESSAGE_ID);
  }

  private OrderPaidMessage paidMessage() {
    return new OrderPaidMessage(
        MESSAGE_ID,
        "trace-1",
        "order.paid",
        Instant.parse("2026-01-01T00:00:00Z"),
        new OrderPaidMessage.OrderPaidData(42L));
  }

  private Message rawMessage(long deliveryTag) {
    MessageProperties properties = new MessageProperties();
    properties.setDeliveryTag(deliveryTag);
    return new Message(new byte[0], properties);
  }
}
