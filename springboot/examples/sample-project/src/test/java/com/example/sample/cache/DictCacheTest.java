package com.example.sample.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sample.common.CacheKeys;
import com.example.sample.common.DictStatus;
import com.example.sample.entity.DictItem;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 字典缓存单测：key `dict:{typeCode}`、带 TTL、类型不符即视为未命中、变更后删缓存。 */
@ExtendWith(MockitoExtension.class)
class DictCacheTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private ValueOperations<String, Object> valueOperations;

  private DictCache dictCache;

  @BeforeEach
  void setUp() {
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    dictCache = new DictCache(redisTemplate);
  }

  @Test
  void shouldReturnCachedItemsWhenPresent() {
    when(valueOperations.get("dict:CHANNEL")).thenReturn(new ArrayList<>(List.of(item("APP"))));

    assertThat(dictCache.findItems("CHANNEL")).isPresent();
    assertThat(dictCache.findItems("CHANNEL").orElseThrow()).hasSize(1);
  }

  @Test
  void shouldDropUnexpectedElementsFromCachedList() {
    when(valueOperations.get("dict:CHANNEL"))
        .thenReturn(new ArrayList<>(Arrays.asList(item("APP"), "junk")));

    assertThat(dictCache.findItems("CHANNEL").orElseThrow())
        .singleElement()
        .extracting(DictItem::getItemCode)
        .isEqualTo("APP");
  }

  @Test
  void shouldReturnEmptyWhenCachedValueIsNotAList() {
    when(valueOperations.get("dict:CHANNEL")).thenReturn("junk");

    assertThat(dictCache.findItems("CHANNEL")).isEmpty();
  }

  @Test
  void shouldPutItemsWithTtl() {
    dictCache.putItems("CHANNEL", List.of(item("APP")));

    verify(valueOperations).set(eq("dict:CHANNEL"), any(List.class), any(Duration.class));
  }

  @Test
  void shouldStoreMutableArrayListSoJsonKeepsTypeInfo() {
    dictCache.putItems("CHANNEL", List.of(item("APP")));

    verify(valueOperations)
        .set(
            eq("dict:CHANNEL"),
            argThat(value -> value instanceof ArrayList<?>),
            any(Duration.class));
  }

  @Test
  void shouldEvictImmediatelyWhenNoTransactionIsActive() {
    dictCache.evictItemsAfterCommit("CHANNEL");

    verify(redisTemplate).delete("dict:CHANNEL");
  }

  @Test
  void shouldEvictOnlyAfterTransactionCommit() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      dictCache.evictItemsAfterCommit("CHANNEL");

      verify(redisTemplate, never()).delete(anyString());
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      verify(redisTemplate).delete("dict:CHANNEL");
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldBuildDictKeyFromTypeCode() {
    assertThat(CacheKeys.dict("CHANNEL")).isEqualTo("dict:CHANNEL");
  }

  private DictItem item(String itemCode) {
    DictItem item = new DictItem();
    item.setTypeCode("CHANNEL");
    item.setItemCode(itemCode);
    item.setItemLabel("应用商店");
    item.setStatus(DictStatus.ENABLED);
    return item;
  }
}
