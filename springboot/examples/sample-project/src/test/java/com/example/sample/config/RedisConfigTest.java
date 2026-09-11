package com.example.sample.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.sample.common.DictStatus;
import com.example.sample.common.OrderStatus;
import com.example.sample.entity.DictItem;
import com.example.sample.entity.Order;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/** Redis 序列化/装配单测：JSON（禁 JDK 序列化）、时间类型可往返、key 为 String。 */
class RedisConfigTest {

  private final RedisConfig redisConfig = new RedisConfig();

  @Test
  void shouldSerializeValueAsJsonInsteadOfJdkSerialization() {
    GenericJackson2JsonRedisSerializer serializer = redisConfig.redisValueSerializer();

    byte[] bytes = serializer.serialize(order());
    String json = new String(bytes, StandardCharsets.UTF_8);

    assertThat(json).startsWith("{");
    assertThat(json).contains("@class").contains("com.example.sample.entity.Order");
    assertThat(json).contains("CREATED");
    // JDK 序列化魔数 0xACED —— 必须不出现
    assertThat(bytes[0] & 0xFF).isNotEqualTo(0xAC);
  }

  @Test
  void shouldRoundTripEntityWithEnumAndLocalDateTime() {
    GenericJackson2JsonRedisSerializer serializer = redisConfig.redisValueSerializer();
    Order order = order();

    Object restored = serializer.deserialize(serializer.serialize(order));

    assertThat(restored).isInstanceOf(Order.class);
    assertThat(restored).usingRecursiveComparison().isEqualTo(order);
  }

  @Test
  void shouldRoundTripDictItemList() {
    GenericJackson2JsonRedisSerializer serializer = redisConfig.redisValueSerializer();
    DictItem item = new DictItem();
    item.setId(1L);
    item.setTypeCode("CHANNEL");
    item.setItemCode("APP");
    item.setItemLabel("应用商店");
    item.setStatus(DictStatus.ENABLED);

    Object restored = serializer.deserialize(serializer.serialize(new ArrayList<>(List.of(item))));

    assertThat(restored).isInstanceOf(List.class);
    assertThat((List<?>) restored).singleElement().isInstanceOf(DictItem.class);
  }

  @Test
  void shouldOnlyEmitTypeInfoForNonFinalCollectionTypes() {
    GenericJackson2JsonRedisSerializer serializer = redisConfig.redisValueSerializer();
    DictItem item = new DictItem();
    item.setItemCode("APP");

    String immutableJson = new String(serializer.serialize(List.of(item)), StandardCharsets.UTF_8);
    String arrayListJson =
        new String(serializer.serialize(new ArrayList<>(List.of(item))), StandardCharsets.UTF_8);

    // List.of/Collections.emptyList 是 final 类：NON_FINAL 策略不写类型信息，读回 Object 会失败
    // ⇒ DictCache 写入前统一复制为 ArrayList（见
    // DictCacheTest#shouldStoreMutableArrayListSoJsonKeepsTypeInfo）
    assertThat(immutableJson).startsWith("[{");
    assertThat(arrayListJson).startsWith("[\"java.util.ArrayList\"");
  }

  @Test
  void shouldWireStringKeysWithJsonValues() {
    RedisTemplate<String, Object> template =
        redisConfig.redisTemplate(
            mock(RedisConnectionFactory.class), redisConfig.redisValueSerializer());

    assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
    assertThat(template.getValueSerializer())
        .isInstanceOf(GenericJackson2JsonRedisSerializer.class);
  }

  private Order order() {
    Order order = new Order();
    order.setId(1L);
    order.setOrderNo("NO001");
    order.setStatus(OrderStatus.CREATED);
    order.setAmountFen(100L);
    order.setVersion(0);
    order.setDeleted(0);
    order.setCreateTime(LocalDateTime.of(2026, 1, 1, 10, 0));
    order.setUpdateTime(LocalDateTime.of(2026, 1, 1, 10, 0));
    return order;
  }
}
