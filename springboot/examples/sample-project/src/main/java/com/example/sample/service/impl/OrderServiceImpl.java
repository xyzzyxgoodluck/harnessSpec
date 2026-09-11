package com.example.sample.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.example.sample.cache.OrderCache;
import com.example.sample.common.ErrorCode;
import com.example.sample.common.OrderStatus;
import com.example.sample.common.PageResult;
import com.example.sample.entity.Order;
import com.example.sample.mapper.OrderMapper;
import com.example.sample.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 订单服务实现：事务边界所在；条件查询一律走 Mapper 自定义方法（Wrapper 不出 mapper 包）。 */
@Service
public class OrderServiceImpl extends CrudRepository<OrderMapper, Order> implements OrderService {

  private final OrderCache orderCache;

  /** 构造器注入 Mapper（同时装配 CrudRepository 的 baseMapper，避免字段注入）。 */
  public OrderServiceImpl(OrderMapper orderMapper, OrderCache orderCache) {
    this.baseMapper = orderMapper;
    this.orderCache = orderCache;
  }

  @Override
  @Transactional
  public Order createOrder(String orderNo, Long amountFen) {
    if (!StringUtils.hasText(orderNo)) {
      throw ErrorCode.PARAM_INVALID.exception("orderNo 不能为空");
    }
    Order order = new Order();
    order.setOrderNo(orderNo);
    order.setAmountFen(amountFen);
    order.setStatus(OrderStatus.CREATED);
    save(order);
    orderCache.evictDetailAfterCommit(order.getId());
    return order;
  }

  @Override
  @Transactional(readOnly = true)
  public Order getOrderById(Long id) {
    return orderCache
        .findDetail(id)
        .orElseGet(
            () -> {
              Order order = getOrderOrThrow(id);
              orderCache.putDetail(order);
              return order;
            });
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<Order> pageOrders(OrderStatus status, long page, long size) {
    IPage<Order> result = baseMapper.selectOrderPage(new Page<>(page, size), status);
    return PageResult.of(
        result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
  }

  @Override
  @Transactional
  public Order cancelOrder(Long id) {
    Order order = getOrderOrThrow(id);
    if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PAID) {
      throw ErrorCode.ORDER_CANNOT_CANCEL.exception(id);
    }
    order.setStatus(OrderStatus.CANCELLED);
    updateById(order);
    orderCache.evictDetailAfterCommit(id);
    return order;
  }

  @Override
  @Transactional
  public Order handleOrderPaid(Long orderId) {
    Order order = getOrderOrThrow(orderId);
    if (order.getStatus() != OrderStatus.CREATED) {
      throw ErrorCode.ORDER_STATUS_NOT_ALLOWED.exception(orderId);
    }
    order.setStatus(OrderStatus.PAID);
    updateById(order);
    orderCache.evictDetailAfterCommit(orderId);
    return order;
  }

  private Order getOrderOrThrow(Long id) {
    Order order = getById(id);
    if (order == null) {
      throw ErrorCode.ORDER_NOT_FOUND.exception(id);
    }
    return order;
  }
}
