package com.example.sample.common;

import java.util.List;

/**
 * 统一分页出参结构（{@code items/total/page/pageSize}）：列表端点一律返回本结构， 禁止把 {@code IPage} 或 entity
 * 直接暴露给外部（CODING_STANDARDS §4/§5）。
 *
 * @param <T> 列表项类型（对外为 *Response）
 */
public record PageResult<T>(List<T> items, long total, long page, long pageSize) {

  /** 由 MP 分页结果/服务层数据构造统一分页结构。 */
  public static <T> PageResult<T> of(List<T> items, long total, long page, long pageSize) {
    return new PageResult<>(items, total, page, pageSize);
  }
}
