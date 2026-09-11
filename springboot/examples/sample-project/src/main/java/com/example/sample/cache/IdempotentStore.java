package com.example.sample.cache;

import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/** 幂等登记：Redis {@code SETNX + TTL}（at-least-once 下重复投递是常态，CODING_STANDARDS §8）。 */
@Component
public class IdempotentStore {

  /** 幂等键保留时长（覆盖消息可能的重投窗口）。 */
  private static final Duration IDEM_TTL = Duration.ofHours(24);

  private static final String REGISTERED = "1";

  private final RedisTemplate<String, Object> redisTemplate;

  /** 构造器注入。 */
  public IdempotentStore(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * 登记幂等键。
   *
   * @param key 幂等键（由消息内容派生，见 {@code CacheKeys}）
   * @return true=首次登记（可继续处理）；false=重复投递（应直接 ack 丢弃）
   */
  public boolean register(String key) {
    return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, REGISTERED, IDEM_TTL));
  }
}
