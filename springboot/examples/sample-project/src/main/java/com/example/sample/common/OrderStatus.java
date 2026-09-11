package com.example.sample.common;

import com.baomidou.mybatisplus.annotation.EnumValue;

/** 订单状态机（稳定状态机用 Java 枚举、不落字典表；落库为字符串 code，CODING_STANDARDS §5）。 */
public enum OrderStatus {
  /** 已创建。 */
  CREATED("CREATED"),
  /** 已支付。 */
  PAID("PAID"),
  /** 已取消。 */
  CANCELLED("CANCELLED");

  /** 落库值：{@code @EnumValue} 令 MP 用本字段读写（需配置 default-enum-type-handler，见 application.yml）。 */
  @EnumValue private final String code;

  OrderStatus(String code) {
    this.code = code;
  }

  /** 落库 code（对应 {@code t_order.status} 列）。 */
  public String code() {
    return code;
  }
}
