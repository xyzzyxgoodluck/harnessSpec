package com.example.sample.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sample.cache.OrderCache;
import com.example.sample.common.BizException;
import com.example.sample.common.ErrorCode;
import com.example.sample.common.OrderStatus;
import com.example.sample.common.PageResult;
import com.example.sample.entity.Order;
import com.example.sample.mapper.OrderMapper;
import com.example.sample.service.impl.OrderServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 订单 Service 单测：mock Mapper 与缓存，覆盖正常路径、每个异常分支（断言 ErrorCode）与边界。 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock private OrderMapper orderMapper;
  @Mock private OrderCache orderCache;

  private OrderServiceImpl orderService;

  @BeforeEach
  void setUp() {
    orderService = new OrderServiceImpl(orderMapper, orderCache);
  }

  @Test
  void shouldCreateOrderWithCreatedStatusAndEvictCache() {
    when(orderMapper.insert(any(Order.class)))
        .thenAnswer(
            invocation -> {
              Order inserted = invocation.getArgument(0);
              inserted.setId(1L);
              return 1;
            });

    Order order = orderService.createOrder("NO001", 100L);

    assertThat(order.getOrderNo()).isEqualTo("NO001");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    verify(orderMapper).insert(order);
    verify(orderCache).evictDetailAfterCommit(1L);
  }

  @Test
  void shouldRejectBlankOrderNo() {
    assertThatThrownBy(() -> orderService.createOrder("  ", 100L))
        .isInstanceOf(BizException.class)
        .extracting(e -> ((BizException) e).getErrorCode())
        .isEqualTo(ErrorCode.PARAM_INVALID);
    verify(orderMapper, never()).insert(any(Order.class));
  }

  @Test
  void shouldReturnCachedOrderWhenCacheHit() {
    Order cached = orderOf(7L, "NO007", OrderStatus.PAID);
    when(orderCache.findDetail(7L)).thenReturn(Optional.of(cached));

    assertThat(orderService.getOrderById(7L)).isSameAs(cached);
    verify(orderMapper, never()).selectById(any());
  }

  @Test
  void shouldLoadFromDbAndFillCacheWhenCacheMiss() {
    Order db = orderOf(8L, "NO008", OrderStatus.CREATED);
    when(orderCache.findDetail(8L)).thenReturn(Optional.empty());
    when(orderMapper.selectById(8L)).thenReturn(db);

    assertThat(orderService.getOrderById(8L)).isSameAs(db);
    verify(orderCache).putDetail(db);
  }

  @Test
  void shouldThrowNotFoundWhenOrderMissing() {
    when(orderCache.findDetail(999L)).thenReturn(Optional.empty());
    when(orderMapper.selectById(999L)).thenReturn(null);

    assertThatThrownBy(() -> orderService.getOrderById(999L))
        .isInstanceOf(BizException.class)
        .extracting(e -> ((BizException) e).getErrorCode())
        .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
  }

  @Test
  void shouldPageOrdersThroughMapperAndMapToPageResult() {
    Page<Order> page = new Page<>(2, 5);
    page.setRecords(List.of(orderOf(1L, "NO001", OrderStatus.CREATED)));
    page.setTotal(11L);
    when(orderMapper.selectOrderPage(any(), eq(OrderStatus.CREATED))).thenReturn(page);

    PageResult<Order> result = orderService.pageOrders(OrderStatus.CREATED, 2, 5);

    assertThat(result.items()).hasSize(1);
    assertThat(result.total()).isEqualTo(11L);
    assertThat(result.page()).isEqualTo(2L);
    assertThat(result.pageSize()).isEqualTo(5L);
  }

  @Test
  void shouldCancelCreatedOrderAndEvictCache() {
    Order order = orderOf(1L, "NO001", OrderStatus.CREATED);
    when(orderMapper.selectById(1L)).thenReturn(order);
    when(orderMapper.updateById(any(Order.class))).thenReturn(1);

    Order cancelled = orderService.cancelOrder(1L);

    assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    verify(orderMapper).updateById(order);
    verify(orderCache).evictDetailAfterCommit(1L);
  }

  @Test
  void shouldRejectCancelWhenStatusNotAllowed() {
    when(orderMapper.selectById(2L)).thenReturn(orderOf(2L, "NO002", OrderStatus.CANCELLED));

    assertThatThrownBy(() -> orderService.cancelOrder(2L))
        .isInstanceOf(BizException.class)
        .extracting(e -> ((BizException) e).getErrorCode())
        .isEqualTo(ErrorCode.ORDER_CANNOT_CANCEL);
    verify(orderMapper, never()).updateById(any(Order.class));
  }

  @Test
  void shouldMarkOrderPaidWhenHandlingPaidEvent() {
    Order order = orderOf(3L, "NO003", OrderStatus.CREATED);
    when(orderMapper.selectById(3L)).thenReturn(order);
    when(orderMapper.updateById(any(Order.class))).thenReturn(1);

    assertThat(orderService.handleOrderPaid(3L).getStatus()).isEqualTo(OrderStatus.PAID);
    verify(orderCache).evictDetailAfterCommit(3L);
  }

  @Test
  void shouldRejectHandlePaidWhenStatusNotCreated() {
    when(orderMapper.selectById(4L)).thenReturn(orderOf(4L, "NO004", OrderStatus.PAID));

    assertThatThrownBy(() -> orderService.handleOrderPaid(4L))
        .isInstanceOf(BizException.class)
        .extracting(e -> ((BizException) e).getErrorCode())
        .isEqualTo(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
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
