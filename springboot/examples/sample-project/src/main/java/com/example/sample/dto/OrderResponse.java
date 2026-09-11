package com.example.sample.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 订单出参（*Response 命名；不直接暴露 entity）。 */
@Schema(description = "订单信息")
public class OrderResponse {

  @Schema(description = "订单 ID", example = "1800000000000000001")
  private Long id;

  @Schema(description = "外部订单号", example = "NO20260101001")
  private String orderNo;

  @Schema(description = "订单状态 code", example = "CREATED")
  private String status;

  @Schema(description = "订单金额（分）", example = "1999")
  private Long amountFen;

  /** 组装订单出参（entity 不出网关，转换集中在 DTO 工厂方法）。 */
  public static OrderResponse of(Long id, String orderNo, String status, Long amountFen) {
    OrderResponse response = new OrderResponse();
    response.id = id;
    response.orderNo = orderNo;
    response.status = status;
    response.amountFen = amountFen;
    return response;
  }

  public Long getId() {
    return id;
  }

  public String getOrderNo() {
    return orderNo;
  }

  public String getStatus() {
    return status;
  }

  public Long getAmountFen() {
    return amountFen;
  }
}
