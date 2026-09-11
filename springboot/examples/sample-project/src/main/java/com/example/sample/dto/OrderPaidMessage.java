package com.example.sample.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 订单支付事件消息（信封顶层字段 messageId/traceId/type/timestamp + 业务载荷 data，CODING_STANDARDS §8）。 */
@Schema(description = "订单支付事件消息")
public record OrderPaidMessage(
    @Schema(description = "消息唯一 ID（幂等键来源）") String messageId,
    @Schema(description = "链路标识") String traceId,
    @Schema(description = "事件类型", example = "order.paid") String type,
    @Schema(description = "事件时间") Instant timestamp,
    @Schema(description = "业务载荷") OrderPaidData data) {

  /**
   * 支付事件业务载荷。
   *
   * @param orderId 订单 ID
   */
  @Schema(description = "支付事件业务载荷")
  public record OrderPaidData(@Schema(description = "订单 ID") Long orderId) {}
}
