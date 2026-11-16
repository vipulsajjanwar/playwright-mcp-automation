/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Coordinates Allure report generation, including serving, opening, and archiving generated reports.
 * Created: 2026-10-07
 */
package framework.reporting.allure;

import framework.config.ReportingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Automates Allure report generation, history synchronization, and server interactions.
 * Works seamlessly in Local, CI/CD (Jenkins, GitHub Actions), and parallel execution modes.
 */
public final class AllureReportManager {

    private static final Logger logger = LoggerFactory.getLogger(AllureReportManager.class);

    private AllureReportManager() {}

    /**
     * Executes 'allure generate <resultsDir> --clean -o <reportDir>' via OS process.
     *
     * @param resultsDir Path to the allure-results folder
     * @param reportDir Path to the target allure-report folder
     * @return true if report generation succeeded, false otherwise
     */
    public static boolean generateReport(Path resultsDir, Path reportDir) {
        if (resultsDir == null || !Files.exists(resultsDir)) {
            logger.warn("Allure results directory does not exist: {}", resultsDir);
            return false;
        }

        logger.info("Generating Allure report from '{}' into '{}'...", resultsDir, reportDir);

        List<String> command = buildAllureCommand("generate", resultsDir.toAbsolutePath().toString(), "-o", reportDir.toAbsolutePath().toString());

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            int exitCode = finished ? process.exitValue() : -1;

            if (exitCode == 0) {
                logger.info("Allure report successfully generated at: {}", reportDir.toAbsolutePath());
                logger.debug("Allure CLI output: \n{}", output);

                // Back up history folder to the suite root execution folder
                Path executionDir = reportDir.getParent();
                if (executionDir != null) {
                    Path backupDir = executionDir.resolve("history");
                    AllureHistoryManager.backupGeneratedHistory(reportDir, backupDir);
                }
                return true;
            } else {
                logger.warn("Allure CLI exited with code {}. Output:\n{}", exitCode, output);
                logger.info("Tip: You can also generate and view reports using the 'run-allure.ps1' PowerShell script.");
                return false;
            }

        } catch (Exception e) {
            logger.warn("Could not execute Allure CLI automatically (is Allure installed and in PATH?): {}", e.getMessage());
            logger.info("To generate reports manually or on Windows, run 'powershell -File ./run-allure.ps1'");
            return false;
        }
    }

    /**
     * Generates report for the specified suite's current execution.
     */
    public static boolean generateReportForSuite(String suiteName) {
        ReportPathManager.ExecutionPaths paths = ReportPathManager.getExecutionPaths(suiteName);
        return generateReport(paths.resultsDir(), paths.reportDir());
    }

    /**
     * Generates report for the latest execution.
     */
    public static boolean generateReportForLatestExecution() {
        ReportPathManager.ExecutionPaths paths = ReportPathManager.getLatestExecutionPaths();
        return generateReport(paths.resultsDir(), paths.reportDir());
    }

    /**
     * Handles suite startup: initializes directories and preserves previous history.
     */
    public static ReportPathManager.ExecutionPaths onSuiteStart(String suiteName) {
        ReportPathManager.ExecutionPaths paths = ReportPathManager.initializeExecutionPaths(suiteName);
        AllureHistoryManager.preserveHistory(paths.suiteName(), paths.resultsDir(), paths.executionDir());
        return paths;
    }

    /**
     * Handles suite finish: automatically generates the report and backups history if enabled.
     */
    public static void onSuiteFinish(String suiteName) {
        if (!ReportingConfig.isAllureEnabled()) {
            return;
        }

        ReportPathManager.ExecutionPaths paths = ReportPathManager.getExecutionPaths(suiteName);

        // Synchronize results written to default staging directory (reports/allure-results) into suite results dir
        try {
            Path defaultResultsDir = java.nio.file.Paths.get("reports/allure-results").toAbsolutePath().normalize();
            if (Files.exists(defaultResultsDir) && !defaultResultsDir.equals(paths.resultsDir().toAbsolutePath().normalize())) {
                AllureHistoryManager.copyDirectoryContents(defaultResultsDir, paths.resultsDir());
            }
        } catch (Exception e) {
            logger.warn("Could not synchronize default allure-results into suite folder: {}", e.getMessage());
        }

        if (ReportingConfig.isAllureAutoGenerateEnabled()) {
            generateReport(paths.resultsDir(), paths.reportDir());
        }
    }

    private static List<String> buildAllureCommand(String... args) {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        List<String> command = new ArrayList<>();

        if (isWindows) {
            command.add("cmd.exe");
            command.add("/c");
            command.add("allure");
        } else {
            command.add("allure");
        }

        for (String arg : args) {
            command.add(arg);
        }

        return command;
    }
}
