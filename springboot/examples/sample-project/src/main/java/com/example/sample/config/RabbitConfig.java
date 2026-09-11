package com.example.sample.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ 拓扑声明（集中于此，不用散落魔法字符串）；死信链路必须"DLX + Binding"成对声明。 */
@Configuration
public class RabbitConfig {

  /** 业务交换机。 */
  @Bean
  public TopicExchange orderExchange() {
    return new TopicExchange(MqDestinations.ORDER_EXCHANGE, true, false);
  }

  /** 死信交换机。 */
  @Bean
  public TopicExchange orderDlx() {
    return new TopicExchange(MqDestinations.ORDER_DLX, true, false);
  }

  /** 订单支付队列：绑定死信交换机与死信 RoutingKey，拒绝的消息才会被真正路由到 DLQ。 */
  @Bean
  public Queue orderPaidQueue() {
    return QueueBuilder.durable(MqDestinations.ORDER_PAID_QUEUE)
        .deadLetterExchange(MqDestinations.ORDER_DLX)
        .deadLetterRoutingKey(MqDestinations.ORDER_PAID_DLQ_RK)
        .build();
  }

  /** 订单支付死信队列。 */
  @Bean
  public Queue orderPaidDlq() {
    return QueueBuilder.durable(MqDestinations.ORDER_PAID_DLQ).build();
  }

  /** 业务队列绑定：exchange ← order.paid。 */
  @Bean
  public Binding orderPaidBinding(TopicExchange orderExchange, Queue orderPaidQueue) {
    return BindingBuilder.bind(orderPaidQueue).to(orderExchange).with(MqDestinations.ORDER_PAID_RK);
  }

  /** 死信队列绑定：DLX ← order.paid.dlq（缺此绑定则死信进 DLX 后无路由被静默丢弃）。 */
  @Bean
  public Binding orderPaidDlqBinding(TopicExchange orderDlx, Queue orderPaidDlq) {
    return BindingBuilder.bind(orderPaidDlq).to(orderDlx).with(MqDestinations.ORDER_PAID_DLQ_RK);
  }

  /** 消息体统一 JSON（禁 JDK 序列化）；复用应用 ObjectMapper 以支持 Java 时间类型。 */
  @Bean
  public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }
}
