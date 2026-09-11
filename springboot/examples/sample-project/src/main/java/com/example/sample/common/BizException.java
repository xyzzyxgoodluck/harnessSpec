package com.example.sample.common;

/** 业务异常：携带 {@link ErrorCode}（可预期、可向用户展示；继承 RuntimeException 以便默认事务回滚）。 */
public class BizException extends RuntimeException {

  private final ErrorCode errorCode;

  /**
   * 构造业务异常。
   *
   * @param errorCode 错误码（唯一登记表条目）
   * @param message 已按业务上下文填充的对外消息（禁止在调用处再做拼接）
   */
  public BizException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  /** 错误码。 */
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
