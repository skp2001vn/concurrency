package org.example.parallelreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.parallelreport.ParallelReportGenerator.FailedSection;
import org.example.parallelreport.ParallelReportGenerator.ReportResult;
import org.example.parallelreport.ParallelReportGenerator.ReportSection;
import org.example.parallelreport.ParallelReportGenerator.ReportSectionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ParallelReportGeneratorTest {

    private ParallelReportGenerator generator;

    @AfterEach
    void tearDown() {
        if (generator != null) {
            generator.shutdown();
        }
    }

    /**
     * Verifies that all successful section fetches are returned in the report's fixed section
     * order.
     */
    @Test
    void generatesAllSectionsInReportOrder() throws InterruptedException {
        generator = new ParallelReportGenerator(
                3,
                reportId -> "sales:" + reportId,
                reportId -> "inventory:" + reportId,
                reportId -> "billing:" + reportId);

        ReportResult result = generator.generate("weekly-report");

        assertEquals(
                new ReportResult(
                        "weekly-report",
                        List.of(
                                new ReportSection(ReportSectionType.SALES, "sales:weekly-report"),
                                new ReportSection(
                                        ReportSectionType.INVENTORY,
                                        "inventory:weekly-report"),
                                new ReportSection(
                                        ReportSectionType.BILLING,
                                        "billing:weekly-report")),
                        List.of()),
                result);
    }

    /**
     * Verifies that the caller remains blocked until every section task counts down the latch.
     */
    @Test
    void waitsForEverySectionToFinishBeforeReturning() throws InterruptedException {
        CountDownLatch allStarted = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger concurrentSections = new AtomicInteger();
        AtomicInteger maxConcurrentSections = new AtomicInteger();

        generator = new ParallelReportGenerator(
                3,
                reportId -> blockUntilReleased(
                        "sales:" + reportId,
                        allStarted,
                        release,
                        concurrentSections,
                        maxConcurrentSections),
                reportId -> blockUntilReleased(
                        "inventory:" + reportId,
                        allStarted,
                        release,
                        concurrentSections,
                        maxConcurrentSections),
                reportId -> blockUntilReleased(
                        "billing:" + reportId,
                        allStarted,
                        release,
                        concurrentSections,
                        maxConcurrentSections));

        CompletableFuture<ReportResult> future =
                CompletableFuture.supplyAsync(() -> generateUnchecked("weekly-report"));

        assertTrue(allStarted.await(1, TimeUnit.SECONDS));
        assertFalse(future.isDone());
        assertEquals(3, maxConcurrentSections.get());

        release.countDown();

        assertEquals(3, future.join().sections().size());
    }

    /**
     * Verifies that one failed section does not prevent the other sections from being collected.
     */
    @Test
    void collectsFailuresWithoutDroppingSuccessfulSections() throws InterruptedException {
        generator = new ParallelReportGenerator(
                3,
                reportId -> "sales:" + reportId,
                reportId -> {
                    throw new IllegalStateException("inventory source unavailable");
                },
                reportId -> "billing:" + reportId);

        ReportResult result = generator.generate("weekly-report");

        assertEquals(
                List.of(
                        new ReportSection(ReportSectionType.SALES, "sales:weekly-report"),
                        new ReportSection(ReportSectionType.BILLING, "billing:weekly-report")),
                result.sections());
        assertEquals(
                List.of(new FailedSection(
                        ReportSectionType.INVENTORY,
                        "IllegalStateException",
                        "inventory source unavailable")),
                result.failures());
    }

    /**
     * Verifies that invalid worker counts are rejected up front.
     */
    @Test
    void rejectsNonPositiveWorkerCount() {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> new ParallelReportGenerator(
                        0,
                        reportId -> "sales",
                        reportId -> "inventory",
                        reportId -> "billing"));

        assertEquals("workerCount must be positive", thrown.getMessage());
    }

    private ReportResult generateUnchecked(String reportId) {
        try {
            return generator.generate(reportId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private String blockUntilReleased(
            String content,
            CountDownLatch allStarted,
            CountDownLatch release,
            AtomicInteger concurrentSections,
            AtomicInteger maxConcurrentSections) {
        int currentSections = concurrentSections.incrementAndGet();
        maxConcurrentSections.accumulateAndGet(currentSections, Math::max);
        allStarted.countDown();

        try {
            if (!release.await(1, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release section fetches");
            }
            return content;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            concurrentSections.decrementAndGet();
        }
    }
}
