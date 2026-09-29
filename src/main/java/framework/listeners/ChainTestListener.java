package framework.listeners;

import com.microsoft.playwright.Page;
import framework.config.ChainTestConfig;
import framework.config.ConfigManager;
import framework.reporting.ChainTestContext;
import framework.reporting.ChainTestLogger;
import framework.reporting.LogLevel;
import framework.reporting.TestStatus;
import framework.utils.ChainTestUtil;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChainTestListener implements ITestListener {

    private static final ThreadLocal<ChainTestContext> CURRENT_TEST =
            new ThreadLocal<>();

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern(ConfigManager.get("timestamp.format", "yyyy-MM-dd HH:mm:ss"));

    // ============================================================
    // ENABLE / DISABLE
    // ============================================================

    /**
     * Returns whether ChainTest reporting is enabled.
     *
     * Priority:
     *
     * 1. JVM argument:
     *    -Dchaintest.enabled=true
     *
     * 2. chaintest.properties
     *
     * 3. false
     */

    // ============================================================
    // TEST LIFECYCLE
    // ============================================================

    /**
     * Starts a ChainTest test.
     */
    public static void startTest(String testName) {

        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        // Prevent accidental duplicate test creation
        endCurrentTestIfRequired();

        ChainTestContext context =
                new ChainTestContext(
                        testName,
                        Thread.currentThread().getId()
                );

        CURRENT_TEST.set(context);

        // --------------------------------------------------------
        // ACTUAL CHAINTTEST TEST CREATION
        // --------------------------------------------------------
        //
        // Put your ChainTest test creation call here.
        //
        // Example conceptually:
        //
        // context.setChainTest(
        //     <ChainTest test object>
        // );
        //
        // --------------------------------------------------------

        info(
                "========== TEST STARTED: "
                        + testName
                        + " =========="
        );
    }

    /**
     * Ends the current test successfully.
     */
    public static void passTest() {

        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        ChainTestContext context =
                CURRENT_TEST.get();

        if (context == null) {
            return;
        }

        context.setStatus(TestStatus.PASS);

        passStep(
                "Test passed: "
                        + context.getTestName()
        );

        finishCurrentTest();
    }

    /**
     * Marks the current test as failed.
     */
    public static void failTest(
            Throwable throwable,
            Page page) {

        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        ChainTestContext context =
                CURRENT_TEST.get();

        if (context == null) {
            return;
        }

        context.setStatus(TestStatus.FAIL);

        String message =
                throwable != null
                        ? throwable.getMessage()
                        : "Test execution failed";

        error(
                "Test failed: " + message
        );

        if (page != null &&
                isScreenshotOnFailureEnabled()) {

            attachScreenshot(
                    "Failure Screenshot",
                    page
            );
        }

        finishCurrentTest();
    }

    /**
     * Marks the current test as skipped.
     */
    public static void skipTest(
            String reason) {

        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        ChainTestContext context =
                CURRENT_TEST.get();

        if (context == null) {
            return;
        }

        context.setStatus(TestStatus.SKIP);

        warn(
                "Test skipped: "
                        + reason
        );

        finishCurrentTest();
    }

    /**
     * Ends the current test without forcing a status.
     */
    public static void endTest() {

        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        finishCurrentTest();
    }

    // ============================================================
    // TESTNG INTEGRATION
    // ============================================================

    /**
     * Handles a TestNG result automatically.
     * <p>
     * Call this from @AfterMethod or your TestNG listener.
     */
    public static void handleTestResult(
            ITestResult result,
            Page page) {

        if (!ChainTestUtil.isEnabled() || result == null) {
            return;
        }

        switch (result.getStatus()) {

            case ITestResult.SUCCESS:

                passTest();
                break;

            case ITestResult.FAILURE:

                failTest(
                        result.getThrowable(),
                        page
                );
                break;

            case ITestResult.SKIP:

                String reason =
                        result.getThrowable() != null
                                ? result.getThrowable()
                                .getMessage()
                                : "Test skipped";

                skipTest(reason);
                break;

            default:

                endTest();
                break;
        }
    }

    // ============================================================
    // LOGGING
    // ============================================================

    /**
     * INFO log.
     */
    public static void info(
            String message) {

        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        log(
                LogLevel.INFO,
                message
        );
    }

    /**
     * PASS log.
     */
    public static void passStep(
            String message) {

        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        log(
                LogLevel.PASS,
                message
        );
    }

    /**
     * WARNING log.
     */
    public static void warn(
            String message) {

        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        log(
                LogLevel.WARN,
                message
        );
    }

    /**
     * ERROR log.
     */
    public static void error(
            String message) {

        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        log(
                LogLevel.ERROR,
                message
        );
    }

    /**
     * FAIL log.
     */
    public static void fail(
            String message) {

        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        log(
                LogLevel.FAIL,
                message
        );
    }

    /**
     * Central logging method.
     * <p>
     * IMPORTANT:
     * Do not use System.out.println here.
     * <p>
     * Otherwise you can get duplicate console/report output when
     * ChainTest/TestNG logging is also enabled.
     */
    private static void log(
            LogLevel level,
            String message) {

        ChainTestContext context =
                CURRENT_TEST.get();

        if (context == null) {

            // Don't silently lose framework logs.
            // But don't create a new ChainTest test either.
            return;
        }

        String formattedMessage =
                "[" + level.name() + "] "
                        + message;

        context.incrementLogCount();

        // --------------------------------------------------------
        // ACTUAL CHAINTTEST LOGGING
        // --------------------------------------------------------
        //
        // Delegate to one place only.
        //
        ChainTestLogger.log(
                level,
                formattedMessage
        );
    }

    // ============================================================
    // SCREENSHOT
    // ============================================================

    /**
     * Captures a Playwright screenshot and stores it under the
     * ChainTest report directory.
     */
    public static Path attachScreenshot(
            String name,
            Page page) {

        if (!ChainTestUtil.isEnabled() || page == null) {
            return null;
        }

        try {

            Path directory =
                    getScreenshotDirectory();

            Files.createDirectories(directory);

            String fileName =
                    sanitizeFileName(name)
                            + "_"
                            + timestamp()
                            + ".png";

            Path screenshotPath =
                    directory.resolve(fileName);

            byte[] screenshot =
                    page.screenshot(
                            new Page.ScreenshotOptions()
                                    .setFullPage(true)
                    );

            Files.write(
                    screenshotPath,
                    screenshot
            );

            ChainTestContext context =
                    CURRENT_TEST.get();

            if (context != null) {

                context.addAttachment(
                        screenshotPath
                );
            }

            // ----------------------------------------------------
            // ACTUAL CHAINTTEST ATTACHMENT
            // ----------------------------------------------------
            //
            // Attach screenshotPath to ChainTest here.
            //
            // ----------------------------------------------------

            return screenshotPath;

        } catch (Exception e) {

            error(
                    "Unable to capture screenshot: "
                            + e.getMessage()
            );

            return null;
        }
    }

    // ============================================================
    // TEXT ATTACHMENT
    // ============================================================

    /**
     * Creates a text attachment.
     */
    public static Path attachText(
            String name,
            String content) {

        if (!ChainTestUtil.isEnabled()) {
            return null;
        }

        try {

            Path directory =
                    getAttachmentDirectory();

            Files.createDirectories(directory);

            String fileName =
                    sanitizeFileName(name)
                            + "_"
                            + timestamp()
                            + ".txt";

            Path file =
                    directory.resolve(fileName);

            Files.writeString(
                    file,
                    content != null
                            ? content
                            : ""
            );

            ChainTestContext context =
                    CURRENT_TEST.get();

            if (context != null) {
                context.addAttachment(file);
            }

            // ----------------------------------------------------
            // ACTUAL CHAINTTEST ATTACHMENT
            // ----------------------------------------------------

            return file;

        } catch (Exception e) {

            error(
                    "Unable to create text attachment: "
                            + e.getMessage()
            );

            return null;
        }
    }

    // ============================================================
    // METADATA
    // ============================================================

    public static void addMetadata(
            String key,
            String value) {

        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        ChainTestContext context =
                CURRENT_TEST.get();

        if (context == null) {
            return;
        }

        context.addMetadata(
                key,
                value
        );

        // --------------------------------------------------------
        // ACTUAL CHAINTTEST METADATA CALL
        // --------------------------------------------------------
    }

    public static void addBrowser(
            String browser) {

        addMetadata(
                "Browser",
                browser
        );
    }

    public static void addEnvironment(
            String environment) {

        addMetadata(
                "Environment",
                environment
        );
    }

    public static void addTestClass(
            String className) {

        addMetadata(
                "Test Class",
                className
        );
    }

    // ============================================================
    // CURRENT TEST
    // ============================================================

    public static String getCurrentTestName() {

        ChainTestContext context =
                CURRENT_TEST.get();

        return context != null
                ? context.getTestName()
                : null;
    }

    public static boolean hasActiveTest() {

        return CURRENT_TEST.get() != null;
    }

    // ============================================================
    // INTERNAL FINISH
    // ============================================================

    private static void finishCurrentTest() {

        ChainTestContext context =
                CURRENT_TEST.get();

        if (context == null) {
            return;
        }

        info(
                "========== TEST FINISHED: "
                        + context.getTestName()
                        + " =========="
        );

        // --------------------------------------------------------
        // ACTUAL CHAINTTEST TEST FINALIZATION
        // --------------------------------------------------------

        CURRENT_TEST.remove();
    }

    private static void endCurrentTestIfRequired() {

        ChainTestContext context =
                CURRENT_TEST.get();

        if (context != null) {
            finishCurrentTest();
        }
    }

    // ============================================================
    // FILE UTILITIES
    // ============================================================

    private static Path getReportDirectory() {

        String configuredPath =
                ChainTestConfig.get(
                        "chaintest.report.path",
                        "reports/chaintest"
                );

        return Paths.get(
                configuredPath
        );
    }

    private static Path getScreenshotDirectory() {

        return getReportDirectory()
                .resolve("screenshots");
    }

    private static Path getAttachmentDirectory() {

        return getReportDirectory()
                .resolve("attachments");
    }

    private static boolean
    isScreenshotOnFailureEnabled() {

        return ChainTestConfig.getBoolean(
                "chaintest.screenshot.on.failure",
                true
        );
    }

    private static String timestamp() {

        return LocalDateTime.now()
                .format(TIMESTAMP_FORMAT);
    }

    private static String sanitizeFileName(
            String value) {

        if (value == null ||
                value.isBlank()) {

            return "attachment";
        }

        return value
                .replaceAll(
                        "[^a-zA-Z0-9._-]",
                        "_"
                );
    }


}
