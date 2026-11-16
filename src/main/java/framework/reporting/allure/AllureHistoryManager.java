/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Manages Allure history trend data: copying previous results, pruning old runs, and linking history for trend charts.
 * Created: 2026-10-07
 */
package framework.reporting.allure;

import framework.config.ReportingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Manages Allure history persistence across test suite runs.
 *
 * <p>Flow:
 * 1. Before new test execution / report generation:
 *    Locate previous execution folder -> copy `allure-report/history` (or `history`) to `new-execution/allure-results/history`.
 * 2. This preserves:
 *    - Trend charts (Success rate, Failures, Flaky, Broken)
 *    - Execution history timeline
 *    - Retry trends
 *    - Duration charts
 *    - Category defect history
 * 3. After Allure report is generated:
 *    Backup `allure-report/history` into `new-execution/history`.
 */
public final class AllureHistoryManager {

    private static final Logger logger = LoggerFactory.getLogger(AllureHistoryManager.class);

    private AllureHistoryManager() {}

    /**
     * Preserves history by copying past execution history to the current results directory.
     *
     * @param suiteName Name of the running suite
     * @param currentResultsDir Path to the current execution's allure-results folder
     * @param currentExecutionDir Path to the current execution's parent folder (to prevent self-copying)
     * @return true if previous history was found and copied, false otherwise
     */
    public static boolean preserveHistory(String suiteName, Path currentResultsDir, Path currentExecutionDir) {
        if (!ReportingConfig.isAllureHistoryEnabled()) {
            logger.info("Allure history preservation is disabled by configuration (allure.history.enabled=false).");
            return false;
        }

        try {
            Optional<Path> previousExecutionDirOpt = ReportPathManager.findLatestPreviousExecutionDir(suiteName, currentExecutionDir);

            if (previousExecutionDirOpt.isEmpty()) {
                logger.info("No previous execution folder found for suite '{}'. Starting a new Allure history trend.", suiteName);
                return false;
            }

            Path previousExecutionDir = previousExecutionDirOpt.get();
            Path sourceHistoryDir = resolveSourceHistoryDir(previousExecutionDir);

            if (sourceHistoryDir == null || !Files.exists(sourceHistoryDir) || !Files.isDirectory(sourceHistoryDir)) {
                logger.info("Previous execution found at '{}', but no history folder exists. Starting fresh history.", previousExecutionDir);
                return false;
            }

            Path targetHistoryDir = currentResultsDir.resolve("history");
            Files.createDirectories(targetHistoryDir);

            int copiedFiles = copyDirectoryContents(sourceHistoryDir, targetHistoryDir);

            // Also mirror history to reports/allure-results/history if separate
            Path defaultResultsHistory = java.nio.file.Paths.get("reports/allure-results/history");
            if (!defaultResultsHistory.toAbsolutePath().normalize().equals(targetHistoryDir.toAbsolutePath().normalize())) {
                try {
                    Files.createDirectories(defaultResultsHistory);
                    copyDirectoryContents(sourceHistoryDir, defaultResultsHistory);
                } catch (Exception ignored) {}
            }

            if (copiedFiles > 0) {
                logger.info("Successfully copied {} Allure history files from '{}' to '{}'",
                        copiedFiles, sourceHistoryDir, targetHistoryDir);
                return true;
            } else {
                logger.info("Source history folder '{}' was empty.", sourceHistoryDir);
                return false;
            }

        } catch (Exception e) {
            logger.warn("Failed to preserve Allure history for suite '{}': {}", suiteName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Backs up the newly generated history from allure-report to the execution history backup directory.
     *
     * @param reportDir The directory where allure-report was generated
     * @param backupDir The backup directory to store history in
     * @return true if backup succeeded, false otherwise
     */
    public static boolean backupGeneratedHistory(Path reportDir, Path backupDir) {
        if (reportDir == null || backupDir == null) {
            return false;
        }

        try {
            Path reportHistoryDir = reportDir.resolve("history");
            if (!Files.exists(reportHistoryDir) || !Files.isDirectory(reportHistoryDir)) {
                logger.debug("No history directory found in generated report at '{}'", reportDir);
                return false;
            }

            Files.createDirectories(backupDir);
            int copiedCount = copyDirectoryContents(reportHistoryDir, backupDir);
            logger.info("Backed up {} Allure history artifacts to '{}'", copiedCount, backupDir);
            return true;
        } catch (Exception e) {
            logger.warn("Could not backup generated Allure history to '{}': {}", backupDir, e.getMessage());
            return false;
        }
    }

    private static Path resolveSourceHistoryDir(Path previousExecutionDir) {
        // Priority 1: allure-report/history
        Path reportHistory = previousExecutionDir.resolve("allure-report").resolve("history");
        if (Files.exists(reportHistory) && Files.isDirectory(reportHistory)) {
            return reportHistory;
        }

        // Priority 2: direct history/ backup folder
        Path directHistory = previousExecutionDir.resolve("history");
        if (Files.exists(directHistory) && Files.isDirectory(directHistory)) {
            return directHistory;
        }

        // Priority 3: allure-results/history (if report wasn't generated yet)
        Path resultsHistory = previousExecutionDir.resolve("allure-results").resolve("history");
        if (Files.exists(resultsHistory) && Files.isDirectory(resultsHistory)) {
            return resultsHistory;
        }

        return null;
    }

    public static int copyDirectoryContents(Path sourceDir, Path targetDir) throws IOException {
        int count = 0;
        try (Stream<Path> stream = Files.list(sourceDir)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                Path dest = targetDir.resolve(file.getFileName());
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                count++;
            }
        }
        return count;
    }
}
