package com.example.sample.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sample.common.ErrorCode;
import com.example.sample.common.OrderStatus;
import com.example.sample.common.PageResult;
import com.example.sample.entity.Order;
import com.example.sample.service.OrderService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Controller 切片测试：@WebMvcTest + mock Service（@MockitoBean），无需中间件。 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private OrderService orderService;

  @Test
  void shouldCreateOrderAndCarryTraceId() throws Exception {
    when(orderService.createOrder(eq("NO001"), eq(100L)))
        .thenReturn(orderOf(1L, "NO001", OrderStatus.CREATED));

    mockMvc
        .perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"NO001\",\"amountFen\":100}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.orderNo").value("NO001"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  @Test
  void shouldRejectInvalidCreateWithParamErrorCode() throws Exception {
    mockMvc
        .perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"\",\"amountFen\":-1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void shouldGetOrderById() throws Exception {
    when(orderService.getOrderById(1L)).thenReturn(orderOf(1L, "NO001", OrderStatus.CREATED));

    mockMvc
        .perform(get("/orders/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value(OrderStatus.CREATED.code()));
  }

  @Test
  void shouldReturn404WithBusinessCodeWhenOrderNotFound() throws Exception {
    when(orderService.getOrderById(9L)).thenThrow(ErrorCode.ORDER_NOT_FOUND.exception(9L));

    mockMvc
        .perform(get("/orders/9"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorCode.ORDER_NOT_FOUND.code()))
        .andExpect(jsonPath("$.message").value("订单不存在: id=9"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  @Test
  void shouldPageOrdersWithUnifiedPageResult() throws Exception {
    when(orderService.pageOrders(isNull(), eq(1L), eq(10L)))
        .thenReturn(PageResult.of(List.of(orderOf(1L, "NO001", OrderStatus.CREATED)), 1L, 1L, 10L));

    mockMvc
        .perform(get("/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.pageSize").value(10))
        .andExpect(jsonPath("$.data.items[0].orderNo").value("NO001"));
  }

  @Test
  void shouldCapPageSizeAtUpperLimit() throws Exception {
    when(orderService.pageOrders(isNull(), eq(1L), eq(100L)))
        .thenReturn(PageResult.of(List.of(), 0L, 1L, 100L));

    mockMvc
        .perform(get("/orders").param("size", "100000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.pageSize").value(100));
  }

  @Test
  void shouldRejectUnknownStatusValue() throws Exception {
    mockMvc
        .perform(get("/orders").param("status", "NOT_A_STATUS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()));
  }

  @Test
  void shouldCancelOrder() throws Exception {
    when(orderService.cancelOrder(1L)).thenReturn(orderOf(1L, "NO001", OrderStatus.CANCELLED));

    mockMvc
        .perform(post("/orders/1/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value(OrderStatus.CANCELLED.code()));
  }

  @Test
  void shouldMapExternalDependencyFailureTo5xxxxx() throws Exception {
    when(orderService.getOrderById(anyLong()))
        .thenThrow(new DataAccessResourceFailureException("db down"));

    mockMvc
        .perform(get("/orders/1"))
        .andExpect(status().isGatewayTimeout())
        .andExpect(jsonPath("$.code").value(ErrorCode.THIRD_PARTY_TIMEOUT.code()))
        .andExpect(jsonPath("$.message").value(ErrorCode.THIRD_PARTY_TIMEOUT.template()))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  @Test
  void shouldNotLeakDetailsOnUnknownError() throws Exception {
    when(orderService.getOrderById(anyLong())).thenThrow(new IllegalStateException("SQL 语句泄漏表名"));

    mockMvc
        .perform(get("/orders/1"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value(ErrorCode.SYSTEM_ERROR.code()))
        .andExpect(jsonPath("$.message").value(ErrorCode.SYSTEM_ERROR.template()))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  @Test
  void shouldAcceptUpstreamTraceId() throws Exception {
    when(orderService.getOrderById(anyLong()))
        .thenReturn(orderOf(1L, "NO001", OrderStatus.CREATED));

    mockMvc
        .perform(get("/orders/1").header("X-Trace-Id", "trace-from-upstream"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.traceId").value("trace-from-upstream"));
  }

  private Order orderOf(Long id, String orderNo, OrderStatus status) {
    Order order = new Order();
    order.setId(id);
    order.setOrderNo(orderNo);
    order.setStatus(status);
    order.setAmountFen(100L);
    return order;
  }
}
