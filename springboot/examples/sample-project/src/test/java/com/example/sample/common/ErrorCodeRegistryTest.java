package com.example.sample.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** 错误码注册表测试：把"分段/命名/查重/只增不删"的登记纪律固化为可执行断言（CODING_STANDARDS §6）。 */
class ErrorCodeRegistryTest {

  /** 已登记错误码快照（name → code）：删除/改号/新增未登记都会让测试变红，强制"先查重再登记"。 */
  private static final Map<String, Integer> REGISTERED = registeredSnapshot();

  private static final Set<Integer> KNOWN_SEGMENTS = Set.of(1, 2, 3, 4, 5);

  @Test
  void shouldUseSixDigitCodeWithKnownSegmentPrefix() {
    for (ErrorCode errorCode : ErrorCode.values()) {
      assertThat(String.valueOf(errorCode.code()))
          .as("错误码 %s 必须是 6 位数字", errorCode.name())
          .hasSize(6)
          .matches("\\d{6}");
      assertThat(KNOWN_SEGMENTS)
          .as("错误码 %s 的首位必须是 1/2/3/4/5 之一", errorCode.name())
          .contains(segmentOf(errorCode));
    }
  }

  @Test
  void shouldNotDuplicateCodes() {
    List<Integer> codes = new ArrayList<>();
    for (ErrorCode errorCode : ErrorCode.values()) {
      codes.add(errorCode.code());
    }

    assertThat(codes).doesNotHaveDuplicates();
  }

  @Test
  void shouldUseUpperSnakeCaseNames() {
    for (ErrorCode errorCode : ErrorCode.values()) {
      assertThat(errorCode.name())
          .as("错误码名必须为 UPPER_SNAKE_CASE，禁 E001/FAIL 之类无信息量命名")
          .matches("^[A-Z][A-Z0-9_]*$");
    }
  }

  @Test
  void shouldCoverEverySegment() {
    Set<Integer> usedSegments =
        List.of(ErrorCode.values()).stream()
            .map(ErrorCodeRegistryTest::segmentOf)
            .collect(Collectors.toCollection(TreeSet::new));

    assertThat(usedSegments)
        .as("1系统/2参数/3业务/4认证/5外部依赖 每段都必须有登记（外部依赖段如 THIRD_PARTY_TIMEOUT）")
        .containsExactlyElementsOf(new TreeSet<>(KNOWN_SEGMENTS));
  }

  @Test
  void shouldMapSegmentToExpectedHttpStatusClass() {
    for (ErrorCode errorCode : ErrorCode.values()) {
      HttpStatus status = errorCode.httpStatus();
      switch (segmentOf(errorCode)) {
        case 1 -> assertThat(status.is5xxServerError()).as(errorCode.name()).isTrue();
        case 2 ->
            assertThat(status)
                .as(errorCode.name())
                .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNPROCESSABLE_ENTITY);
        case 3 -> assertThat(status.is4xxClientError()).as(errorCode.name()).isTrue();
        case 4 ->
            assertThat(status)
                .as(errorCode.name())
                .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        default ->
            assertThat(status)
                .as(errorCode.name())
                .isIn(
                    HttpStatus.BAD_GATEWAY,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    HttpStatus.GATEWAY_TIMEOUT);
      }
    }
  }

  @Test
  void shouldHaveNonBlankMessageTemplate() {
    for (ErrorCode errorCode : ErrorCode.values()) {
      assertThat(errorCode.template()).as(errorCode.name()).isNotBlank();
      assertThat(errorCode.message()).as(errorCode.name()).isNotBlank();
    }
  }

  @Test
  void shouldFillMessageTemplateWithBusinessContext() {
    assertThat(ErrorCode.ORDER_NOT_FOUND.message(42L)).isEqualTo("订单不存在: id=42");
    assertThat(ErrorCode.PARAM_INVALID.message("orderNo 不能为空")).isEqualTo("参数错误: orderNo 不能为空");
    assertThat(ErrorCode.DICT_ITEM_NOT_FOUND.message("CHANNEL", "APP"))
        .isEqualTo("字典项不存在: typeCode=CHANNEL itemCode=APP");
  }

  @Test
  void shouldFillTemplateLiterallyEvenWhenArgContainsRegexMeta() {
    assertThat(ErrorCode.PARAM_INVALID.message("$1\\d")).isEqualTo("参数错误: $1\\d");
  }

  @Test
  void shouldBuildBizExceptionCarryingRegisteredCode() {
    BizException exception = ErrorCode.ORDER_STATUS_NOT_ALLOWED.exception(7L);

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
    assertThat(exception.getMessage()).isEqualTo("订单状态不允许该操作: id=7");
  }

  @Test
  void shouldMatchRegisteredSnapshot() {
    Map<String, Integer> actual = new LinkedHashMap<>();
    for (ErrorCode errorCode : ErrorCode.values()) {
      actual.put(errorCode.name(), errorCode.code());
    }

    assertThat(actual)
        .as("错误码只增不删；新增/删除请同步维护本测试的 REGISTERED 快照并先查重")
        .containsExactlyEntriesOf(REGISTERED);
  }

  private static int segmentOf(ErrorCode errorCode) {
    return errorCode.code() / 100000;
  }

  private static Map<String, Integer> registeredSnapshot() {
    Map<String, Integer> snapshot = new LinkedHashMap<>();
    snapshot.put("SYSTEM_ERROR", 100001);
    snapshot.put("PARAM_INVALID", 200001);
    snapshot.put("ORDER_NOT_FOUND", 310001);
    snapshot.put("ORDER_CANNOT_CANCEL", 310002);
    snapshot.put("ORDER_STATUS_NOT_ALLOWED", 310003);
    snapshot.put("DICT_TYPE_NOT_FOUND", 310004);
    snapshot.put("DICT_ITEM_NOT_FOUND", 310005);
    snapshot.put("UNAUTHORIZED", 400001);
    snapshot.put("FORBIDDEN", 400002);
    snapshot.put("THIRD_PARTY_TIMEOUT", 500001);
    return snapshot;
  }
}
