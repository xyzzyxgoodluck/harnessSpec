package com.example.sample.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 统一响应结构（{@code code=0} 表示成功；错误码见 {@link ErrorCode}）。错误响应不含 {@code data}。 */
public class Result<T> {

  private int code;
  private String message;

  /** 链路标识：与本次请求日志同源，便于排障（CODING_STANDARDS §6/§9）。 */
  private String traceId;

  /** 成功数据；错误响应为 null 且不参与序列化。 */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private T data;

  /** 成功响应（携带 {@code traceId}）。 */
  public static <T> Result<T> ok(T data) {
    Result<T> result = new Result<>();
    result.code = 0;
    result.message = "ok";
    result.traceId = TraceId.current();
    result.data = data;
    return result;
  }

  /** 无数据的成功响应。 */
  public static Result<Void> ok() {
    return ok(null);
  }

  /** 失败响应（{@code data} 不输出；{@code traceId} 与日志同源）。 */
  public static <T> Result<T> fail(int code, String message) {
    Result<T> result = new Result<>();
    result.code = code;
    result.message = message;
    result.traceId = TraceId.current();
    return result;
  }

  /** 业务状态码，0=成功。 */
  public int getCode() {
    return code;
  }

  /** 面向用户的消息。 */
  public String getMessage() {
    return message;
  }

  /** 链路标识。 */
  public String getTraceId() {
    return traceId;
  }

  /** 成功数据。 */
  public T getData() {
    return data;
  }
}
