package com.example.sample.cache;

import com.example.sample.common.AfterCommit;
import com.example.sample.common.CacheKeys;
import com.example.sample.entity.DictItem;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/** 字典项缓存（key {@code dict:{typeCode}}，带 TTL）：字典只读高频，读回源回填、变更后删缓存。 */
@Component
public class DictCache {

  /** 字典项基础 TTL（CODING_STANDARDS §7）。 */
  private static final Duration ITEMS_TTL = Duration.ofMinutes(30);

  /** TTL 随机抖动上限（秒）。 */
  private static final int TTL_JITTER_SECONDS = 120;

  private final RedisTemplate<String, Object> redisTemplate;

  /** 构造器注入。 */
  public DictCache(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /** 读字典项缓存；未命中或类型不符返回空。 */
  public Optional<List<DictItem>> findItems(String typeCode) {
    Object value = redisTemplate.opsForValue().get(CacheKeys.dict(typeCode));
    if (!(value instanceof List<?> raw)) {
      return Optional.empty();
    }
    List<DictItem> items =
        raw.stream().filter(DictItem.class::isInstance).map(DictItem.class::cast).toList();
    return Optional.of(items);
  }

  /** 回填字典项缓存（TTL 带随机抖动）。 */
  public void putItems(String typeCode, List<DictItem> items) {
    // JDK 不可变集合（List.of/Collections.emptyList）是 final 类，DefaultTyping.NON_FINAL 不写类型信息，
    // 反序列化到 Object 时会失败；统一复制成 ArrayList 保证缓存可读回（见 RedisConfigTest）。
    List<DictItem> cacheable = new ArrayList<>(items);
    redisTemplate.opsForValue().set(CacheKeys.dict(typeCode), cacheable, itemsTtl());
  }

  /** 字典变更后删缓存（事务提交后执行，避免脏读）。 */
  public void evictItemsAfterCommit(String typeCode) {
    String key = CacheKeys.dict(typeCode);
    AfterCommit.run(() -> redisTemplate.delete(key));
  }

  private Duration itemsTtl() {
    return ITEMS_TTL.plusSeconds(ThreadLocalRandom.current().nextInt(TTL_JITTER_SECONDS));
  }
}
