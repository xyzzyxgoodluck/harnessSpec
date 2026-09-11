package com.example.sample.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/** Redis 配置：key 用 String、value 统一 JSON（禁默认 JDK 序列化，CODING_STANDARDS §7）。 */
@Configuration
public class RedisConfig {

  /** Redis 值序列化器：写 {@code @class} 多态信息但限定在白名单包内（比 JDK 序列化安全、可跨语言读）。 */
  @Bean
  public GenericJackson2JsonRedisSerializer redisValueSerializer() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.activateDefaultTyping(
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.example.sample.")
            .allowIfSubType("java.util.")
            .allowIfSubType("java.time.")
            .build(),
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY);
    return new GenericJackson2JsonRedisSerializer(mapper);
  }

  /** 通用 RedisTemplate（常量类登记 key，禁魔法字符串）。 */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(
      RedisConnectionFactory connectionFactory,
      GenericJackson2JsonRedisSerializer redisValueSerializer) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(RedisSerializer.string());
    template.setHashKeySerializer(RedisSerializer.string());
    template.setValueSerializer(redisValueSerializer);
    template.setHashValueSerializer(redisValueSerializer);
    template.afterPropertiesSet();
    return template;
  }
}
