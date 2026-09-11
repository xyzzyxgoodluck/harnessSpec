package com.example.sample.service;

import com.example.sample.common.OrderStatus;
import com.example.sample.common.PageResult;
import com.example.sample.entity.Order;

/** 订单服务：用例动词命名（create/get/page/cancel/handle…）；只声明本域用例，不继承 MP 通用抽象。 */
public interface OrderService {

  /** 创建订单：落库为 CREATED 状态，并删除订单详情缓存。 */
  Order createOrder(String orderNo, Long amountFen);

  /** 单条查询：优先读缓存，未命中回源并回填。 */
  Order getOrderById(Long id);

  /** 分页查询订单；status 为空表示不按状态过滤。 */
  PageResult<Order> pageOrders(OrderStatus status, long page, long size);

  /** 取消订单；状态不允许时抛业务异常（ErrorCode.ORDER_CANNOT_CANCEL）。 */
  Order cancelOrder(Long id);

  /** 处理订单支付成功（MQ 消费入口；重复投递由监听器按幂等键去重）。 */
  Order handleOrderPaid(Long orderId);
}
