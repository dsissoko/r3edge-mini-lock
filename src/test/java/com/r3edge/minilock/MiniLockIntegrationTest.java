
package com.r3edge.minilock;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MiniLockIntegrationTest {

	@Autowired
	ExecutionLockService lockService;

	@Test
	void should_execute_task_if_not_locked() {
		AtomicBoolean executed = new AtomicBoolean(false);

		boolean result = lockService.runIfUnlocked("lock-42", Duration.ofSeconds(5), () -> executed.set(true));

		assertThat(result).isTrue();
		assertThat(executed.get()).isTrue();
	}
}
