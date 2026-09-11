package com.example.sample.common;

import java.util.UUID;
import org.slf4j.MDC;

/** traceId 工具：MDC 键名、生成与绑定，保证 HTTP/MQ/日志/错误响应四处同源（CODING_STANDARDS §9）。 */
public final class TraceId {

  /** MDC 键名，日志 pattern 通过 {@code %X{traceId}} 引用。 */
  public static final String MDC_KEY = "traceId";

  /** 上游透传用的 HTTP 头名。 */
  public static final String HEADER = "X-Trace-Id";

  private TraceId() {}

  /** 生成 32 位无横线 UUID 作为新的 traceId。 */
  public static String generate() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /** 当前线程的 traceId；不在请求/消费上下文时返回空串。 */
  public static String current() {
    String value = MDC.get(MDC_KEY);
    return value == null ? "" : value;
  }

  /** 绑定 traceId 到当前线程的 MDC。 */
  public static void bind(String traceId) {
    MDC.put(MDC_KEY, traceId);
  }

  /** 清理 MDC（线程池复用必须清理，否则串号）。 */
  public static void clear() {
    MDC.remove(MDC_KEY);
  }
}
