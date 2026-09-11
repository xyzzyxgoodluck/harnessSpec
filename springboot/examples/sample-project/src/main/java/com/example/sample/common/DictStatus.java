package com.example.sample.common;

/** 字典启停状态取值（落库为 TINYINT；字典只停用不删除，CODING_STANDARDS §5）。 */
public final class DictStatus {

  /** 启用。 */
  public static final int ENABLED = 1;

  /** 停用。 */
  public static final int DISABLED = 0;

  private DictStatus() {}
}
