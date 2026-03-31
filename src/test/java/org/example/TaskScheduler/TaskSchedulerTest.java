package org.example.TaskScheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TaskSchedulerTest {

    private TaskScheduler scheduler;

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Test
    void executesTaskAfterDelay() throws InterruptedException {
        scheduler = new TaskScheduler();
        CountDownLatch ran = new CountDownLatch(1);
        long start = System.currentTimeMillis();
        long[] executedAt = new long[1];

        scheduler.schedule(() -> {
            executedAt[0] = System.currentTimeMillis();
            ran.countDown();
        }, 150);

        assertTrue(ran.await(1, TimeUnit.SECONDS));
        assertTrue(executedAt[0] - start >= 100);
    }

    @Test
    void executesEarlierTasksBeforeLaterTasks() throws InterruptedException {
        scheduler = new TaskScheduler();
        List<String> order = new CopyOnWriteArrayList<>();
        CountDownLatch completed = new CountDownLatch(3);

        scheduler.schedule(() -> {
            order.add("late");
            completed.countDown();
        }, 250);
        scheduler.schedule(() -> {
            order.add("early");
            completed.countDown();
        }, 50);
        scheduler.schedule(() -> {
            order.add("middle");
            completed.countDown();
        }, 150);

        assertTrue(completed.await(2, TimeUnit.SECONDS));
        assertEquals(List.of("early", "middle", "late"), order);
    }

    @Test
    void rejectsNewTasksAfterShutdown() {
        scheduler = new TaskScheduler();
        scheduler.shutdown();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> scheduler.schedule(() -> {}, 10));

        assertEquals("Scheduler is shut down", thrown.getMessage());
    }
}
