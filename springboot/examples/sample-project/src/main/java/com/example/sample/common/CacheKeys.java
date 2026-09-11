package com.example.sample.common;

/** Redis key 常量（`域:对象:标识`，全小写冒号分隔；集中登记，禁魔法字符串散落，CODING_STANDARDS §7）。 */
public final class CacheKeys {

  /** 订单详情缓存前缀（写库后删缓存）。 */
  public static final String ORDER_DETAIL = "order:detail:";

  /** 字典项缓存前缀（key 形如 {@code dict:CHANNEL}，带 TTL，字典变更后删）。 */
  public static final String DICT = "dict:";

  /** 订单支付消息幂等键前缀（SETNX + TTL）。 */
  public static final String IDEM_ORDER_PAID = "idem:order:paid:";

  private CacheKeys() {}

  /** 订单详情缓存 key：{@code order:detail:{id}}。 */
  public static String orderDetail(Long id) {
    return ORDER_DETAIL + id;
  }

  /** 字典项缓存 key：{@code dict:{typeCode}}。 */
  public static String dict(String typeCode) {
    return DICT + typeCode;
  }

  /** 订单支付消息幂等键：{@code idem:order:paid:{messageId}}。 */
  public static String idemOrderPaid(String messageId) {
    return IDEM_ORDER_PAID + messageId;
  }
}
