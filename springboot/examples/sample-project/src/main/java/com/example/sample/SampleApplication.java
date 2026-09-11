package com.example.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 应用入口（springboot 类型规范的最小真实样例：订单 + 字典，含 Redis/RabbitMQ 链路）。 */
@SpringBootApplication
public class SampleApplication {

  /** 启动 Spring Boot 应用。 */
  public static void main(String[] args) {
    SpringApplication.run(SampleApplication.class, args);
  }
}
