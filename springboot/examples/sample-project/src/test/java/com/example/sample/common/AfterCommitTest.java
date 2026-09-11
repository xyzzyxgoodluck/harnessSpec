package com.example.sample.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 事务后置动作单测：无事务立即执行；有事务只在提交后执行，回滚则完全不执行。 */
class AfterCommitTest {

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldRunImmediatelyWhenNoTransactionIsActive() {
    AtomicInteger counter = new AtomicInteger();

    AfterCommit.run(counter::incrementAndGet);

    assertThat(counter).hasValue(1);
  }

  @Test
  void shouldDeferUntilCommitWhenTransactionIsActive() {
    AtomicInteger counter = new AtomicInteger();
    TransactionSynchronizationManager.initSynchronization();

    AfterCommit.run(counter::incrementAndGet);
    assertThat(counter).hasValue(0);

    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);

    assertThat(counter).hasValue(1);
  }

  @Test
  void shouldSkipWhenTransactionRollsBack() {
    AtomicInteger counter = new AtomicInteger();
    TransactionSynchronizationManager.initSynchronization();

    AfterCommit.run(counter::incrementAndGet);
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(
            synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

    assertThat(counter).hasValue(0);
  }
}
