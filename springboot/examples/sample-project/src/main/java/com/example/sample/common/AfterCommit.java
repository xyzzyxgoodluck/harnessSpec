package com.example.sample.common;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 事务后置动作工具：把"写库后删缓存"从事务内挪到提交之后，避免回滚后缓存已被删（CODING_STANDARDS §7）。 */
public final class AfterCommit {

  private AfterCommit() {}

  /** 有活动事务时注册到提交后执行；无事务（如单测/只读调用）时立即执行。 */
  public static void run(Runnable action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
    } else {
      action.run();
    }
  }
}
