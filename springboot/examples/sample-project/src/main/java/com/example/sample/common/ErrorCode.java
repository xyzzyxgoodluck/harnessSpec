package com.example.sample.common;

import java.util.Objects;
import java.util.regex.Matcher;
import org.springframework.http.HttpStatus;

/** 错误码登记表（全库唯一来源）：6 位数字、首位=大类（1系统/2参数/3业务/4认证/5外部依赖）， 命名 {@code 域_对象_原因}，只增不删、新增先查重。 */
public enum ErrorCode {
  SYSTEM_ERROR(100001, HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后重试"),
  PARAM_INVALID(200001, HttpStatus.BAD_REQUEST, "参数错误: {}"),
  ORDER_NOT_FOUND(310001, HttpStatus.NOT_FOUND, "订单不存在: id={}"),
  ORDER_CANNOT_CANCEL(310002, HttpStatus.CONFLICT, "订单状态不允许取消: id={}"),
  ORDER_STATUS_NOT_ALLOWED(310003, HttpStatus.CONFLICT, "订单状态不允许该操作: id={}"),
  DICT_TYPE_NOT_FOUND(310004, HttpStatus.NOT_FOUND, "字典类型不存在: typeCode={}"),
  DICT_ITEM_NOT_FOUND(310005, HttpStatus.NOT_FOUND, "字典项不存在: typeCode={} itemCode={}"),
  UNAUTHORIZED(400001, HttpStatus.UNAUTHORIZED, "未登录或登录已过期"),
  FORBIDDEN(400002, HttpStatus.FORBIDDEN, "无权限访问"),
  THIRD_PARTY_TIMEOUT(500001, HttpStatus.GATEWAY_TIMEOUT, "外部依赖超时，请稍后重试");

  private final int code;
  private final HttpStatus httpStatus;
  private final String template;

  ErrorCode(int code, HttpStatus httpStatus, String template) {
    this.code = code;
    this.httpStatus = httpStatus;
    this.template = template;
  }

  /** 6 位错误码。 */
  public int code() {
    return code;
  }

  /** 与错误类别匹配的建议 HTTP 状态（CODING_STANDARDS §6 错误分类表）。 */
  public HttpStatus httpStatus() {
    return httpStatus;
  }

  /** 未填充参数的对外消息模板（占位符为 {@code {}}）。 */
  public String template() {
    return template;
  }

  /** 用业务上下文填充占位符后的对外消息（消息参数化，禁止在调用处拼接字符串）。 */
  public String message(Object... args) {
    String filled = template;
    for (Object arg : args) {
      filled =
          filled.replaceFirst("\\{\\}", Matcher.quoteReplacement(Objects.toString(arg, "null")));
    }
    return filled;
  }

  /** 用业务上下文构造业务异常（供 Service 直接 {@code throw}）。 */
  public BizException exception(Object... args) {
    return new BizException(this, message(args));
  }
}
