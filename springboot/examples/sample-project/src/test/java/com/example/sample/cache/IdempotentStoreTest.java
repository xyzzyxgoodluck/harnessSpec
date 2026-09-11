package com.example.sample.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** 幂等登记单测：SETNX 成功=首次处理；失败/无返回=重复投递（必须丢弃）。 */
@ExtendWith(MockitoExtension.class)
class IdempotentStoreTest {

  private static final String KEY = "idem:order:paid:msg-1";

  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private ValueOperations<String, Object> valueOperations;

  private IdempotentStore idempotentStore;

  @BeforeEach
  void setUp() {
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    idempotentStore = new IdempotentStore(redisTemplate);
  }

  @Test
  void shouldReturnTrueWhenFirstRegistered() {
    when(valueOperations.setIfAbsent(eq(KEY), eq("1"), any(Duration.class))).thenReturn(true);

    assertThat(idempotentStore.register(KEY)).isTrue();
  }

  @Test
  void shouldReturnFalseWhenKeyAlreadyExists() {
    when(valueOperations.setIfAbsent(eq(KEY), eq("1"), any(Duration.class))).thenReturn(false);

    assertThat(idempotentStore.register(KEY)).isFalse();
  }

  @Test
  void shouldTreatNullResultAsDuplicate() {
    when(valueOperations.setIfAbsent(eq(KEY), eq("1"), any(Duration.class))).thenReturn(null);

    assertThat(idempotentStore.register(KEY)).isFalse();
  }
}
