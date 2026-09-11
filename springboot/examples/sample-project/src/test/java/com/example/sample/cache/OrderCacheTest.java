package com.example.sample.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sample.common.OrderStatus;
import com.example.sample.entity.Order;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 订单缓存单测：读回源、带 TTL 回填、"写库后删缓存"从事务提交后执行（可注入 RedisTemplate 直接断言）。 */
@ExtendWith(MockitoExtension.class)
class OrderCacheTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private ValueOperations<String, Object> valueOperations;

  private OrderCache orderCache;

  @BeforeEach
  void setUp() {
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    orderCache = new OrderCache(redisTemplate);
  }

  @Test
  void shouldReturnCachedOrderWhenPresent() {
    Order order = orderOf(1L);
    when(valueOperations.get("order:detail:1")).thenReturn(order);

    assertThat(orderCache.findDetail(1L)).containsSame(order);
  }

  @Test
  void shouldReturnEmptyWhenCachedValueHasUnexpectedType() {
    when(valueOperations.get("order:detail:1")).thenReturn("not-an-order");

    assertThat(orderCache.findDetail(1L)).isEmpty();
  }

  @Test
  void shouldPutDetailWithTtlAndJitter() {
    orderCache.putDetail(orderOf(2L));

    verify(valueOperations).set(eq("order:detail:2"), any(Order.class), any(Duration.class));
  }

  @Test
  void shouldEvictImmediatelyWhenNoTransactionIsActive() {
    orderCache.evictDetailAfterCommit(3L);

    verify(redisTemplate).delete("order:detail:3");
  }

  @Test
  void shouldEvictOnlyAfterTransactionCommit() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      orderCache.evictDetailAfterCommit(4L);

      verify(redisTemplate, never()).delete(anyString());
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      verify(redisTemplate).delete("order:detail:4");
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldNotEvictWhenTransactionRollsBack() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      orderCache.evictDetailAfterCommit(5L);

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(
              synchronization ->
                  synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
      verify(redisTemplate, never()).delete(anyString());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldExposeDetailKeyFromCacheKeys() {
    assertThat(orderCache.findDetail(6L)).isEqualTo(Optional.empty());
  }

  private Order orderOf(Long id) {
    Order order = new Order();
    order.setId(id);
    order.setOrderNo("NO" + id);
    order.setStatus(OrderStatus.CREATED);
    return order;
  }
}
