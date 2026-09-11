package com.example.sample.cache;

import com.example.sample.common.AfterCommit;
import com.example.sample.common.CacheKeys;
import com.example.sample.entity.Order;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/** 订单缓存：读回源回填（带 TTL + 随机抖动防雪崩），写库后删缓存（事务提交后执行）。 */
@Component
public class OrderCache {

  /** 订单详情基础 TTL（CODING_STANDARDS §7：缓存必须带 TTL）。 */
  private static final Duration DETAIL_TTL = Duration.ofMinutes(10);

  /** TTL 随机抖动上限（秒），避免同一时刻批量失效造成雪崩。 */
  private static final int TTL_JITTER_SECONDS = 60;

  private final RedisTemplate<String, Object> redisTemplate;

  /** 构造器注入。 */
  public OrderCache(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /** 读订单详情缓存；未命中返回空（由调用方回源 DB 并回填）。 */
  public Optional<Order> findDetail(Long id) {
    Object value = redisTemplate.opsForValue().get(CacheKeys.orderDetail(id));
    return value instanceof Order order ? Optional.of(order) : Optional.empty();
  }

  /** 回填订单详情缓存（TTL 带随机抖动）。 */
  public void putDetail(Order order) {
    redisTemplate.opsForValue().set(CacheKeys.orderDetail(order.getId()), order, detailTtl());
  }

  /** 写库后删缓存：注册到事务提交后执行，避免"事务回滚但缓存已删"。 */
  public void evictDetailAfterCommit(Long id) {
    String key = CacheKeys.orderDetail(id);
    AfterCommit.run(() -> redisTemplate.delete(key));
  }

  private Duration detailTtl() {
    return DETAIL_TTL.plusSeconds(ThreadLocalRandom.current().nextInt(TTL_JITTER_SECONDS));
  }
}
