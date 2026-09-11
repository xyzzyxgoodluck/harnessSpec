package com.example.sample.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** traceId 单测：生成、绑定/清理、以及与统一响应结构同源。 */
class TraceIdTest {

  @AfterEach
  void tearDown() {
    TraceId.clear();
  }

  @Test
  void shouldGenerate32HexChars() {
    assertThat(TraceId.generate()).hasSize(32).matches("[0-9a-f]{32}");
  }

  @Test
  void shouldReturnEmptyWhenNotBound() {
    assertThat(TraceId.current()).isEmpty();
  }

  @Test
  void shouldBindAndClearForThreadReuse() {
    TraceId.bind("trace-1");
    assertThat(TraceId.current()).isEqualTo("trace-1");

    TraceId.clear();
    assertThat(TraceId.current()).isEmpty();
  }

  @Test
  void shouldPropagateTraceIdIntoResponseEnvelope() {
    TraceId.bind("trace-abc");

    assertThat(Result.ok("data").getTraceId()).isEqualTo("trace-abc");
    assertThat(Result.fail(ErrorCode.SYSTEM_ERROR.code(), "boom").getTraceId())
        .isEqualTo("trace-abc");
  }
}
