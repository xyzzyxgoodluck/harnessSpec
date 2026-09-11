package com.example.sample.controller;

import com.example.sample.common.OrderStatus;
import com.example.sample.common.PageResult;
import com.example.sample.common.Result;
import com.example.sample.dto.OrderCreateRequest;
import com.example.sample.dto.OrderResponse;
import com.example.sample.entity.Order;
import com.example.sample.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 订单 Controller：薄（校验→Service→DTO 出参）；REST 路径资源复数 /orders。 */
@Tag(name = "订单")
@RestController
@RequestMapping("/orders")
public class OrderController {

  /** 分页默认页码。 */
  private static final int DEFAULT_PAGE = 1;

  /** 分页默认每页条数。 */
  private static final int DEFAULT_SIZE = 10;

  /** 每页条数上限：列表查询必须有上限，禁"全表捞内存"（CODING_STANDARDS §5）。 */
  private static final int MAX_SIZE = 100;

  private final OrderService orderService;

  /** 构造器注入。 */
  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  /** 创建订单。 */
  @Operation(summary = "创建订单")
  @PostMapping
  public Result<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
    Order order = orderService.createOrder(request.getOrderNo(), request.getAmountFen());
    return Result.ok(toResponse(order));
  }

  /** 按 ID 查询订单（不存在返回 404 + 3xxxxx 业务码）。 */
  @Operation(summary = "查询订单详情")
  @GetMapping("/{id}")
  public Result<OrderResponse> getById(
      @Parameter(description = "订单 ID", example = "1800000000000000001") @PathVariable Long id) {
    return Result.ok(toResponse(orderService.getOrderById(id)));
  }

  /** 分页查询订单（page 从 1 开始，size 超上限自动收敛）。 */
  @Operation(summary = "分页查询订单")
  @GetMapping
  public Result<PageResult<OrderResponse>> pageOrders(
      @Parameter(description = "订单状态 code", example = "CREATED") @RequestParam(required = false)
          OrderStatus status,
      @Parameter(description = "页码，从 1 开始", example = "1") @RequestParam(defaultValue = "1")
          int page,
      @Parameter(description = "每页条数，最大 100", example = "10") @RequestParam(defaultValue = "10")
          int size) {
    int safeSize = Math.min(Math.max(size, DEFAULT_PAGE), MAX_SIZE);
    PageResult<Order> result =
        orderService.pageOrders(status, Math.max(page, DEFAULT_PAGE), safeSize);
    return Result.ok(
        PageResult.of(
            result.items().stream().map(this::toResponse).toList(),
            result.total(),
            result.page(),
            result.pageSize()));
  }

  /** 取消订单（状态不允许返回 409 + 3xxxxx）。 */
  @Operation(summary = "取消订单")
  @PostMapping("/{id}/cancel")
  public Result<OrderResponse> cancel(@PathVariable Long id) {
    return Result.ok(toResponse(orderService.cancelOrder(id)));
  }

  private OrderResponse toResponse(Order order) {
    return OrderResponse.of(
        order.getId(), order.getOrderNo(), order.getStatus().code(), order.getAmountFen());
  }
}
