package com.example.sample.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 全局异常处理：业务错误→业务码、参数错误→2xxxxx、外部依赖→5xxxxx、未知→1xxxxx 且不泄漏堆栈。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** 业务错误：按 ErrorCode 的建议状态返回（WARN，一般不告警）。 */
  @ExceptionHandler(BizException.class)
  public ResponseEntity<Result<Void>> handleBiz(BizException ex) {
    log.warn("业务异常 code={} msg={}", ex.getErrorCode().code(), ex.getMessage());
    return build(ex.getErrorCode().httpStatus(), ex.getErrorCode().code(), ex.getMessage());
  }

  /** 参数/校验错误：体校验与查询参数类型不匹配统一转 2xxxxx（INFO，含字段级提示）。 */
  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class
  })
  public ResponseEntity<Result<Void>> handleValidation(Exception ex) {
    String detail =
        ex instanceof MethodArgumentNotValidException invalid
            ? invalid.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("参数校验失败")
            : "参数格式不正确";
    log.info("参数校验失败: {}", detail);
    return build(
        ErrorCode.PARAM_INVALID.httpStatus(),
        ErrorCode.PARAM_INVALID.code(),
        ErrorCode.PARAM_INVALID.message(detail));
  }

  /** 外部依赖错误（DB/Redis/MQ 不可用或超时）：转 5xxxxx，对外不展示实现细节（ERROR + 告警）。 */
  @ExceptionHandler({DataAccessException.class, AmqpException.class})
  public ResponseEntity<Result<Void>> handleExternalDependency(Exception ex) {
    log.error("外部依赖错误", ex);
    return build(
        ErrorCode.THIRD_PARTY_TIMEOUT.httpStatus(),
        ErrorCode.THIRD_PARTY_TIMEOUT.code(),
        ErrorCode.THIRD_PARTY_TIMEOUT.template());
  }

  /** 未知异常兜底：统一文案 + traceId，堆栈只进日志（ERROR + 告警）。 */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Result<Void>> handleUnknown(Exception ex) {
    log.error("系统错误", ex);
    return build(
        ErrorCode.SYSTEM_ERROR.httpStatus(),
        ErrorCode.SYSTEM_ERROR.code(),
        ErrorCode.SYSTEM_ERROR.template());
  }

  private ResponseEntity<Result<Void>> build(HttpStatus status, int code, String message) {
    return ResponseEntity.status(status).body(Result.fail(code, message));
  }
}
