package org.example.parallelreport;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Generates a report by fetching independent sections in parallel and waiting for all section
 * tasks to finish with a real {@link CountDownLatch}.
 *
 * <p>The generator uses a worker pool for section fetch tasks, while the latch provides the
 * one-shot coordination point that releases the caller only after every section has either
 * succeeded or failed. Successful and failed sections are collected separately to make the
 * coordination role of the latch explicit.
 */
public class ParallelReportGenerator {

    private final SalesSectionService salesSectionService;
    private final InventorySectionService inventorySectionService;
    private final BillingSectionService billingSectionService;
    private final ExecutorService workers;

    /**
     * Creates a report generator with the given worker count and report section providers.
     *
     * @param workerCount the number of worker threads used to fetch report sections
     * @param salesSectionService provider for the sales section
     * @param inventorySectionService provider for the inventory section
     * @param billingSectionService provider for the billing section
     */
    public ParallelReportGenerator(
            int workerCount,
            SalesSectionService salesSectionService,
            InventorySectionService inventorySectionService,
            BillingSectionService billingSectionService) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("workerCount must be positive");
        }
        this.salesSectionService = Objects.requireNonNull(salesSectionService, "salesSectionService");
        this.inventorySectionService =
                Objects.requireNonNull(inventorySectionService, "inventorySectionService");
        this.billingSectionService =
                Objects.requireNonNull(billingSectionService, "billingSectionService");
        this.workers = Executors.newFixedThreadPool(workerCount);
    }

    /**
     * Starts all section fetches for a report and waits until every section task has finished.
     *
     * @param reportId identifier for the report being generated
     * @return immutable successful and failed report sections
     * @throws InterruptedException if the waiting thread is interrupted before all sections finish
     */
    public ReportResult generate(String reportId) throws InterruptedException {
        Objects.requireNonNull(reportId, "reportId");

        CountDownLatch completedSections = new CountDownLatch(ReportSectionType.values().length);
        Map<ReportSectionType, ReportSection> sections = new ConcurrentHashMap<>();
        Map<ReportSectionType, FailedSection> failures = new ConcurrentHashMap<>();

        submitSectionTask(
                reportId,
                ReportSectionType.SALES,
                salesSectionService::fetchSection,
                completedSections,
                sections,
                failures);
        submitSectionTask(
                reportId,
                ReportSectionType.INVENTORY,
                inventorySectionService::fetchSection,
                completedSections,
                sections,
                failures);
        submitSectionTask(
                reportId,
                ReportSectionType.BILLING,
                billingSectionService::fetchSection,
                completedSections,
                sections,
                failures);

        completedSections.await();

        return new ReportResult(
                reportId,
                orderedSections(sections),
                orderedFailures(failures));
    }

    /**
     * Stops the worker pool used for report generation.
     */
    public void shutdown() {
        workers.shutdownNow();
    }

    private void submitSectionTask(
            String reportId,
            ReportSectionType sectionType,
            SectionFetcher sectionFetcher,
            CountDownLatch completedSections,
            Map<ReportSectionType, ReportSection> sections,
            Map<ReportSectionType, FailedSection> failures) {
        workers.submit(() -> {
            try {
                String content = sectionFetcher.fetch(reportId);
                sections.put(sectionType, new ReportSection(sectionType, content));
            } catch (Exception e) {
                failures.put(sectionType, new FailedSection(
                        sectionType,
                        e.getClass().getSimpleName(),
                        e.getMessage() == null ? "" : e.getMessage()));
            } finally {
                completedSections.countDown();
            }
        });
    }

    private List<ReportSection> orderedSections(Map<ReportSectionType, ReportSection> sections) {
        return Arrays.stream(ReportSectionType.values())
                .map(sections::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<FailedSection> orderedFailures(Map<ReportSectionType, FailedSection> failures) {
        return Arrays.stream(ReportSectionType.values())
                .map(failures::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @FunctionalInterface
    private interface SectionFetcher {
        String fetch(String reportId) throws Exception;
    }

    /**
     * Synchronously fetches the sales section for a report.
     */
    @FunctionalInterface
    public interface SalesSectionService {

        /**
         * Returns the rendered sales section for a report.
         *
         * @param reportId identifier for the report
         * @return rendered sales section content
         * @throws Exception if the section fetch fails
         */
        String fetchSection(String reportId) throws Exception;
    }

    /**
     * Synchronously fetches the inventory section for a report.
     */
    @FunctionalInterface
    public interface InventorySectionService {

        /**
         * Returns the rendered inventory section for a report.
         *
         * @param reportId identifier for the report
         * @return rendered inventory section content
         * @throws Exception if the section fetch fails
         */
        String fetchSection(String reportId) throws Exception;
    }

    /**
     * Synchronously fetches the billing section for a report.
     */
    @FunctionalInterface
    public interface BillingSectionService {

        /**
         * Returns the rendered billing section for a report.
         *
         * @param reportId identifier for the report
         * @return rendered billing section content
         * @throws Exception if the section fetch fails
         */
        String fetchSection(String reportId) throws Exception;
    }

    /**
     * The fixed set of report sections produced by the generator.
     */
    public enum ReportSectionType {
        SALES,
        INVENTORY,
        BILLING
    }

    /**
     * Successfully generated report section content.
     *
     * @param type the section kind
     * @param content the rendered section content
     */
    public record ReportSection(ReportSectionType type, String content) {

        public ReportSection {
            type = Objects.requireNonNull(type, "type");
            content = Objects.requireNonNull(content, "content");
        }
    }

    /**
     * Failed report section metadata captured after a section task throws.
     *
     * @param type the section kind that failed
     * @param errorType simple exception type name
     * @param errorMessage exception message captured from the failed task
     */
    public record FailedSection(
            ReportSectionType type,
            String errorType,
            String errorMessage) {

        public FailedSection {
            type = Objects.requireNonNull(type, "type");
            errorType = Objects.requireNonNull(errorType, "errorType");
            errorMessage = Objects.requireNonNull(errorMessage, "errorMessage");
        }
    }

    /**
     * Immutable result of generating a report in parallel.
     *
     * @param reportId identifier for the generated report
     * @param sections successfully generated sections in defined report order
     * @param failures failed sections in defined report order
     */
    public record ReportResult(
            String reportId,
            List<ReportSection> sections,
            List<FailedSection> failures) {

        public ReportResult {
            reportId = Objects.requireNonNull(reportId, "reportId");
            sections = List.copyOf(Objects.requireNonNull(sections, "sections"));
            failures = List.copyOf(Objects.requireNonNull(failures, "failures"));
        }
    }
}
