package com.example.sample.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 创建订单入参（*Request 命名；字段用 Jakarta Validation 注解）。 */
@Schema(description = "创建订单入参")
public class OrderCreateRequest {

  @Schema(
      description = "外部订单号",
      example = "NO20260101001",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "orderNo 不能为空")
  @Size(max = 64, message = "orderNo 长度不能超过 64")
  private String orderNo;

  @Schema(description = "订单金额（分）", example = "1999", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "amountFen 不能为空")
  @Positive(message = "amountFen 必须为正数")
  private Long amountFen;

  public String getOrderNo() {
    return orderNo;
  }

  public void setOrderNo(String orderNo) {
    this.orderNo = orderNo;
  }

  public Long getAmountFen() {
    return amountFen;
  }

  public void setAmountFen(Long amountFen) {
    this.amountFen = amountFen;
  }
}
