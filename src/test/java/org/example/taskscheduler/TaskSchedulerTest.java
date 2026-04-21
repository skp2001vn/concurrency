package org.example.taskscheduler;

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

    /**
     * Verifies that a task executes only after its configured delay.
     */
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

    /**
     * Verifies that tasks run in deadline order rather than submission order.
     */
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

    /**
     * Verifies that scheduling new work after shutdown is rejected.
     */
    @Test
    void rejectsNewTasksAfterShutdown() {
        scheduler = new TaskScheduler();
        scheduler.shutdown();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> scheduler.schedule(() -> {}, 10));

        assertEquals("Scheduler is shut down", thrown.getMessage());
    }

    /**
     * Verifies that submitting a new task is not blocked by another task that is currently running.
     */
    @Test
    void runningTaskDoesNotBlockSubmittingAnotherTask() throws InterruptedException {
        scheduler = new TaskScheduler();
        CountDownLatch longTaskStarted = new CountDownLatch(1);
        CountDownLatch releaseLongTask = new CountDownLatch(1);
        CountDownLatch quickTaskRan = new CountDownLatch(1);

        scheduler.schedule(() -> {
            longTaskStarted.countDown();
            try {
                releaseLongTask.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 0);

        assertTrue(longTaskStarted.await(1, TimeUnit.SECONDS));

        Thread submitter = new Thread(() -> scheduler.schedule(quickTaskRan::countDown, 0));
        submitter.start();
        submitter.join(500);

        assertTrue(!submitter.isAlive());

        releaseLongTask.countDown();
        assertTrue(quickTaskRan.await(1, TimeUnit.SECONDS));
    }
}
