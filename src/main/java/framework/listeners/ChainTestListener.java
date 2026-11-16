/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: TestNG listener bridging framework events to ChainTest real-time reporting dashboard.
 * Created: 2026-09-29
 */
package framework.listeners;

import com.aventstack.chaintest.domain.Embed;
import com.aventstack.chaintest.domain.Result;
import com.aventstack.chaintest.domain.Test;
import com.aventstack.chaintest.service.ChainPluginService;
import com.microsoft.playwright.Page;
import framework.browser.BrowserManager;
import framework.config.ChainTestConfig;
import framework.config.ConfigManager;
import framework.constants.FrameworkConstants;
import framework.constants.TestConstants;
import framework.reporting.*;
import framework.utils.ChainTestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChainTestListener implements ITestListener, ISuiteListener {

//    private static final Logger logger = LoggerFactory.getLogger(ChainTestListener.class);
//
//    private static final ThreadLocal<ChainTestContext> CURRENT_TEST =
//            new ThreadLocal<>();
//
//    private static final ChainPluginService SERVICE = new ChainPluginService("testng");
//    private static final AtomicBoolean SERVICE_STARTED = new AtomicBoolean(false);
//
//    private static final Map<String, Test> SUITES = new ConcurrentHashMap<>();
//    private static final Map<String, Test> CLASSES = new ConcurrentHashMap<>();
//    private static final Set<String> FLUSHED_SUITES = ConcurrentHashMap.newKeySet();
//
//    private static final DateTimeFormatter TIMESTAMP_FORMAT =
//            DateTimeFormatter.ofPattern(ConfigManager.get("timestamp.format", "yyyy-MM-dd HH:mm:ss"));
//
//    private static final String RUN_TIMESTAMP =
//            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
//
//    private static volatile Path chainTestOutputDir;
//    private static volatile Path chainTestHtmlFile;
//    private static volatile Path chainTestEmailFile;

//    private static void prepareChainTestOutput(ISuite suite) {
//        String suiteName = suite != null ? suite.getName() : "Playwright_Automation_Suite";
//        String safeSuiteName = ReportUtils.sanitizeFilename(suiteName);
//
//        chainTestOutputDir = Paths.get("reports", "chaintest", safeSuiteName);
//        chainTestHtmlFile = chainTestOutputDir.resolve(safeSuiteName + "_" + RUN_TIMESTAMP + ".html");
//        chainTestEmailFile = chainTestOutputDir.resolve("Email_" + safeSuiteName + "_" + RUN_TIMESTAMP + ".html");
//
//        System.setProperty("chaintest.generator.simple.output-file", chainTestHtmlFile.toString());
//        System.setProperty("chaintest.generator.email.output-file", chainTestEmailFile.toString());
//        System.setProperty("chaintest.report.path", chainTestOutputDir.toString());
//    }

    private static final Logger logger = LoggerFactory.getLogger(ChainTestListener.class);

    private static final ThreadLocal<ChainTestContext> CURRENT_TEST = new ThreadLocal<>();

    private static final ChainPluginService SERVICE = new ChainPluginService("testng");
    private static final AtomicBoolean SERVICE_STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean REPORT_OUTPUT_PREPARED = new AtomicBoolean(false);
    private static final AtomicBoolean EXECUTION_FINISHED = new AtomicBoolean(false);

    private static final Map<String, Test> SUITES = new ConcurrentHashMap<>();
    private static final Map<String, Test> CLASSES = new ConcurrentHashMap<>();
    private static final Set<String> FLUSHED_SUITES = ConcurrentHashMap.newKeySet();

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern(ConfigManager.get("timestamp.format", "yyyy-MM-dd HH:mm:ss"));

    private static final String RUN_TIMESTAMP =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

    private static volatile Path chainTestOutputDir;
    private static volatile Path chainTestHtmlFile;
    private static volatile Path chainTestEmailFile;
    private static volatile String configuredSuiteName;

    private static final Object TREE_LOCK = new Object();

    private static synchronized void prepareChainTestOutput(ISuite suite) {
        if (REPORT_OUTPUT_PREPARED.get()) {
            if (suite != null
                    && configuredSuiteName != null
                    && !configuredSuiteName.equals(suite.getName())) {
                logger.warn(
                        "ChainTest output is already configured for suite [{}]. "
                                + "Current suite [{}] will use the same JVM-level ChainTest service/report.",
                        configuredSuiteName,
                        suite.getName()
                );
            }
            return;
        }

        String suiteName = (suite != null && suite.getName() != null && !suite.getName().isBlank())
                ? suite.getName()
                : (TestConstants.SuiteName != null && !TestConstants.SuiteName.isBlank()
                ? TestConstants.SuiteName
                : "Playwright_Automation_Suite");

        configuredSuiteName = suiteName;
        String safeSuiteName = ReportUtils.sanitizeFilename(suiteName);

        chainTestOutputDir = Paths.get(FrameworkConstants.REPORT_DIR,"chaintest");//Paths.get("reports", "chaintest", safeSuiteName).toAbsolutePath();
        chainTestHtmlFile = chainTestOutputDir.resolve(safeSuiteName + "_" + RUN_TIMESTAMP + ".html");
        chainTestEmailFile = chainTestOutputDir.resolve("Email_" + safeSuiteName + "_" + RUN_TIMESTAMP + ".html");

        try {
            Files.createDirectories(chainTestOutputDir);
        } catch (Exception e) {
            logger.warn("Unable to create ChainTest output directory {}: {}", chainTestOutputDir, e.getMessage());
        }

        System.setProperty("chaintest.generator.simple.output-file", chainTestHtmlFile.toString());
        System.setProperty("chaintest.generator.email.output-file", chainTestEmailFile.toString());
        System.setProperty("chaintest.report.path", chainTestOutputDir.toString());

        REPORT_OUTPUT_PREPARED.set(true);

        logger.info("ChainTest HTML output configured at: {}", chainTestHtmlFile);
        logger.info("ChainTest email output configured at: {}", chainTestEmailFile);
    }


    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (ChainTestUtil.isEnabled()) {
                    flush();
                }
            } catch (Exception ignored) {
            }
        }));
    }

    private static void ensureServiceStarted() {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        if (!SERVICE_STARTED.compareAndSet(false, true)) {
            return;
        }

        try {
            // Fallback protection in case suite listener order changes
            prepareChainTestOutput(null);

            logger.info("========== ChainTest Service Initialization START ==========");
            long start = System.currentTimeMillis();

            logger.info("Starting ChainPluginService...");
            SERVICE.start();

            logger.info(
                    "ChainPluginService.start() completed in {} ms",
                    System.currentTimeMillis() - start
            );

            SERVICE.addSystemInfo("OS", System.getProperty("os.name"));
            SERVICE.addSystemInfo("Java", System.getProperty("java.version"));

            String env = ConfigManager.get("env");
            if (env != null && !env.isBlank()) {
                SERVICE.addSystemInfo("Environment", env);
            }

            String browser = ConfigManager.get("browser");
            if (browser != null && !browser.isBlank()) {
                SERVICE.addSystemInfo("Browser", browser);
            }

            String baseUrl = ConfigManager.get("baseUrl");
            if (baseUrl != null && !baseUrl.isBlank()) {
                SERVICE.addSystemInfo("Base URL", baseUrl);
            }
        } catch (Exception e) {
            SERVICE_STARTED.set(false);
            logger.warn("Failed to initialize ChainTest service: {}", e.getMessage(), e);
        }
    }


    private static Test getOrCreateSuite(String suiteName) {
        if (suiteName == null || suiteName.isBlank()) {
            suiteName = TestConstants.SuiteName != null && !TestConstants.SuiteName.isBlank()
                    ? TestConstants.SuiteName
                    : "Playwright Test Automation";
        }
        return SUITES.computeIfAbsent(suiteName, name -> {
            ensureServiceStarted();
            return new Test(name);
        });
    }

    private static Test getOrCreateClass(Test suite, String className) {
        final String finalClassName = (className == null || className.isBlank()) ? "DefaultClass" : className;
        final String key = suite.getName() + "::" + finalClassName;

        synchronized (TREE_LOCK) {
            return CLASSES.computeIfAbsent(key, k -> {
                Test classTest = new Test(finalClassName);
                suite.addChild(classTest);
                return classTest;
            });
        }
    }


    // ============================================================
    // TEST LIFECYCLE
    // ============================================================

    public static void startTest(String testName) {
        startTest(testName, null, null, null);
    }

    public static void startTest(String testName, String className, String description, String[] groups) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        try {
            endCurrentTestIfRequired();
            ensureServiceStarted();

            String suiteName = TestConstants.SuiteName != null && !TestConstants.SuiteName.isBlank()
                    ? TestConstants.SuiteName
                    : "Playwright Test Automation";
            Test suite = getOrCreateSuite(suiteName);

            if (className == null || className.isBlank()) {
                className = "DefaultClass";
            }
            Test classTest = getOrCreateClass(suite, className);

            List<String> tags = groups != null ? Arrays.asList(groups) : Collections.emptyList();
            Test methodTest = new Test(testName, Optional.of(className), tags);
            if (description != null && !description.isBlank()) {
                methodTest.setDescription(description);
            }
            classTest.addChild(methodTest);

            ChainTestContext context = new ChainTestContext(testName, Thread.currentThread().threadId());

            context.setChainTestNode(methodTest);
            context.setClassName(className);
            CURRENT_TEST.set(context);

            info("========== TEST STARTED: " + testName + " ==========");
        } catch (Exception e) {
            logger.warn("Failed to start ChainTest for {}: {}", testName, e.getMessage());
        }
    }

    public static void passTest() {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        try {
            ChainTestContext context = CURRENT_TEST.get();
            if (context == null) {
                return;
            }

            context.setStatus(TestStatus.PASS);
            Test test = context.getChainTestNode();
            if (test != null) {
                test.complete();
                test.setResult(Result.PASSED.getResult());
            }

            passStep("Test passed: " + context.getTestName());
            finishCurrentTest();
        } catch (Exception e) {
            logger.warn("Error marking ChainTest as passed: {}", e.getMessage());
        }
    }

    public static void failTest(Throwable throwable, Page page) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        try {
            ChainTestContext context = CURRENT_TEST.get();
            if (context == null) {
                return;
            }

            context.setStatus(TestStatus.FAIL);
            Test test = context.getChainTestNode();
            if (test != null) {
                test.complete(throwable);
                test.setResult(Result.FAILED.getResult());
            }

            String message = throwable != null ? throwable.getMessage() : "Test execution failed";
            error("Test failed: " + message);

            if (page != null && isScreenshotOnFailureEnabled()) {
                attachScreenshot("Failure Screenshot", page);
            }

            finishCurrentTest();
        } catch (Exception e) {
            logger.warn("Error marking ChainTest as failed: {}", e.getMessage());
        }
    }

    public static void skipTest(String reason) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        try {
            if (reason == null || reason.isBlank()) {
                reason = "Test skipped";
            }

            ChainTestContext context = CURRENT_TEST.get();
            if (context == null) {
                return;
            }

            context.setStatus(TestStatus.SKIP);
            Test test = context.getChainTestNode();
            if (test != null) {
                test.complete();
                test.setResult("SKIPPED");
                test.setError(reason);
            }

            warn("Test skipped: " + reason);
            finishCurrentTest();
        } catch (Exception e) {
            logger.warn("Error marking ChainTest as skipped: {}", e.getMessage());
        }
    }

    public static void endTest() {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        finishCurrentTest();
    }

    public static void handleTestResult(ITestResult result, Page page) {
        if (!ChainTestUtil.isEnabled() || result == null) {
            return;
        }

        switch (result.getStatus()) {
            case ITestResult.SUCCESS:
                passTest();
                break;
            case ITestResult.FAILURE:
                failTest(result.getThrowable(), page);
                break;
            case ITestResult.SKIP:
                String reason = result.getThrowable() != null
                        ? result.getThrowable().getMessage()
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

    public static void info(String message) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }
        log(LogLevel.INFO, message);
    }

    public static void passStep(String message) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }
        log(LogLevel.PASS, message);
    }

    public static void warn(String message) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }
        log(LogLevel.WARN, message);
    }

    public static void error(String message) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }
        log(LogLevel.ERROR, message);
    }

    public static void fail(String message) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }
        log(LogLevel.FAIL, message);
    }
    public static void skip(String message) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }
        log(LogLevel.WARN, message);
    }

    public static void debug(String message) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }
        log(LogLevel.DEBUG, message);
    }

    private static void log(LogLevel level, String message) {
        ChainTestContext context = CURRENT_TEST.get();
        if (context == null) {
            return;
        }

        String formattedMessage = "[" + level.name() + "] " + message;
        context.incrementLogCount();
        ChainTestLogger.log(level, formattedMessage);
    }

    // ============================================================
    // SCREENSHOT & ATTACHMENTS
    // ============================================================

    public static Path attachScreenshot(String name, Page page) {
        if (!ChainTestUtil.isEnabled() || page == null) {
            return null;
        }

        try {
            if (page.isClosed()) {
                return null;
            }

            byte[] screenshot = page.screenshot(
                    new Page.ScreenshotOptions().setFullPage(true)
            );

            return attachScreenshotBytes(name, screenshot);
        } catch (Exception e) {
            logger.warn("Unable to capture ChainTest screenshot {}: {}", name, e.getMessage());
            return null;
        }
    }

    public static Path attachScreenshotBytes(String name, byte[] screenshot) {
        if (!ChainTestUtil.isEnabled() || screenshot == null || screenshot.length == 0) {
            return null;
        }

        try {
            Path directory = getScreenshotDirectory();
            Files.createDirectories(directory);

            String fileName = sanitizeFileName(name) + "_" + timestamp() + ".png";
            Path screenshotPath = directory.resolve(fileName);
            Files.write(screenshotPath, screenshot);

            ChainTestContext context = CURRENT_TEST.get();
            if (context != null) {
                context.addAttachment(screenshotPath);
                Test test = context.getChainTestNode();
                if (test != null) {
                    test.addEmbed(new Embed(screenshot, "image/png"));
                }
            }

            return screenshotPath;
        } catch (Exception e) {
            logger.warn("Unable to attach screenshot to ChainTest: {}", e.getMessage());
            return null;
        }
    }

    public static Path attachScreenshotPath(String name, Path screenshotPath) {
        if (!ChainTestUtil.isEnabled() || screenshotPath == null || !Files.exists(screenshotPath)) {
            return null;
        }

        try {
            byte[] bytes = Files.readAllBytes(screenshotPath);

            ChainTestContext context = CURRENT_TEST.get();
            if (context != null) {
                context.addAttachment(screenshotPath);
                Test test = context.getChainTestNode();
                if (test != null) {
                    test.addEmbed(new Embed(bytes, "image/png"));
                }
            }

            return screenshotPath;
        } catch (Exception e) {
            logger.warn("Unable to attach screenshot path to ChainTest: {}", e.getMessage());
            return null;
        }
    }

    public static Path attachText(String name, String content) {
        if (!ChainTestUtil.isEnabled()) {
            return null;
        }

        try {
            Path directory = getAttachmentDirectory();
            Files.createDirectories(directory);

            String fileName = sanitizeFileName(name) + "_" + timestamp() + ".txt";
            Path file = directory.resolve(fileName);
            Files.writeString(file, content != null ? content : "");

            ChainTestContext context = CURRENT_TEST.get();
            if (context != null) {
                context.addAttachment(file);
                Test test = context.getChainTestNode();
                if (test != null) {
                    test.addLog("[" + name + "]\n" + (content != null ? content : ""));
                }
            }

            return file;
        } catch (Exception e) {
            logger.warn("Unable to create text attachment in ChainTest: {}", e.getMessage());
            return null;
        }
    }

    public static Path attachJson(String name, String json) {
        if (!ChainTestUtil.isEnabled()) {
            return null;
        }

        try {
            Path directory = getAttachmentDirectory();
            Files.createDirectories(directory);

            String fileName = sanitizeFileName(name) + "_" + timestamp() + ".json";
            Path file = directory.resolve(fileName);
            Files.writeString(file, json != null ? json : "{}");

            ChainTestContext context = CURRENT_TEST.get();
            if (context != null) {
                context.addAttachment(file);
                Test test = context.getChainTestNode();
                if (test != null) {
                    test.addLog("[" + name + " - JSON]\n" + (json != null ? json : "{}"));
                }
            }

            return file;
        } catch (Exception e) {
            logger.warn("Unable to create JSON attachment in ChainTest: {}", e.getMessage());
            return null;
        }
    }

    public static Path attachHtml(String name, String html) {
        if (!ChainTestUtil.isEnabled()) {
            return null;
        }

        try {
            Path directory = getAttachmentDirectory();
            Files.createDirectories(directory);

            String fileName = sanitizeFileName(name) + "_" + timestamp() + ".html";
            Path file = directory.resolve(fileName);
            Files.writeString(file, html != null ? html : "");

            ChainTestContext context = CURRENT_TEST.get();
            if (context != null) {
                context.addAttachment(file);
                Test test = context.getChainTestNode();
                if (test != null) {
                    test.addLog("[" + name + " - HTML]\n" + (html != null ? html : ""));
                }
            }

            return file;
        } catch (Exception e) {
            logger.warn("Unable to create HTML attachment in ChainTest: {}", e.getMessage());
            return null;
        }
    }

    // ============================================================
    // METADATA
    // ============================================================

    public static void addMetadata(String key, String value) {
        if (!ChainTestUtil.isEnabled() || key == null || value == null) {
            return;
        }

        try {
            ensureServiceStarted();
            SERVICE.addSystemInfo(key, value);

            ChainTestContext context = CURRENT_TEST.get();
            if (context != null) {
                context.addMetadata(key, value);
            }
        } catch (Exception e) {
            logger.warn("Failed to add ChainTest metadata {}: {}", key, e.getMessage());
        }
    }

    public static void addBrowser(String browser) {
        if (browser != null && !browser.isBlank()) {
            addMetadata("Browser", browser);
        }
    }

    public static void addEnvironment(String environment) {
        if (environment != null && !environment.isBlank()) {
            addMetadata("Environment", environment);
        }
    }

    public static void addTestClass(String className) {
        if (!ChainTestUtil.isEnabled() || className == null || className.isBlank()) {
            return;
        }

        addMetadata("Test Class", className);
        ChainTestContext context = CURRENT_TEST.get();
        if (context != null) {
            context.setClassName(className);
            Test methodTest = context.getChainTestNode();
            if (methodTest != null) {
                methodTest.setClassName(className);
                Test currentParent = methodTest.getParent();
                String suiteName = TestConstants.SuiteName != null && !TestConstants.SuiteName.isBlank()
                        ? TestConstants.SuiteName
                        : "Playwright Test Automation";
                Test suite = getOrCreateSuite(suiteName);
                Test targetClass = getOrCreateClass(suite, className);
                if (currentParent != targetClass) {
                    if (currentParent != null) {
                        currentParent.getChildren().remove(methodTest);
                    }
                    targetClass.addChild(methodTest);
                }
            }
        }
    }

    // ============================================================
    // CURRENT TEST / CONTEXT
    // ============================================================

    public static String getCurrentTestName() {
        ChainTestContext context = CURRENT_TEST.get();
        return context != null ? context.getTestName() : null;
    }

    public static boolean hasActiveTest() {
        return CURRENT_TEST.get() != null;
    }

    public static ChainTestContext getCurrentContext() {
        return CURRENT_TEST.get();
    }

    // ============================================================
    // INTERNAL FINISH & REPORT FINALIZATION
    // ============================================================

    private static void finishCurrentTest() {
        ChainTestContext context = CURRENT_TEST.get();
        if (context == null) {
            return;
        }

        try {
            info("========== TEST FINISHED: " + context.getTestName() + " ==========");
        } finally {
            CURRENT_TEST.remove();
        }
    }

    private static void endCurrentTestIfRequired() {
        ChainTestContext context = CURRENT_TEST.get();
        if (context != null) {
            finishCurrentTest();
        }
    }

    public static synchronized void flush() {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        if (!SERVICE_STARTED.get()) {
            return;
        }

        if (!EXECUTION_FINISHED.compareAndSet(false, true)) {
            return;
        }

        try {
            endCurrentTestIfRequired();

            for (Map.Entry<String, Test> entry : SUITES.entrySet()) {
                Test suiteTest = entry.getValue();
                if (FLUSHED_SUITES.add(suiteTest.getName())) {
                    for (Test classTest : suiteTest.getChildren()) {
                        classTest.complete();
                    }
                    suiteTest.complete();
                    SERVICE.afterTest(suiteTest, Optional.empty());
                }
            }

            SERVICE.flush();
            SERVICE.executionFinished();

            if (chainTestHtmlFile != null) {
                logger.info("ChainTest report successfully generated at: {}", chainTestHtmlFile.toAbsolutePath());
            } else if (chainTestOutputDir != null) {
                logger.info("ChainTest report successfully generated under: {}", chainTestOutputDir.toAbsolutePath());
            } else {
                logger.info("ChainTest report successfully generated");
            }
        } catch (Exception e) {
            logger.warn("Failed to finalize ChainTest report: {}", e.getMessage(), e);
        }
    }


    // ============================================================
    // TESTNG LISTENER INTERFACE IMPLEMENTATION
    // ============================================================

    @Override
    public void onTestStart(ITestResult result) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }
        if (!hasActiveTest()) {
            String testName = result.getMethod().getMethodName();
            String className = result.getTestClass().getRealClass().getSimpleName();
            String description = result.getMethod().getDescription();
            String[] groups = result.getMethod().getGroups();
            startTest(testName, className, description, groups);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }
        if (hasActiveTest()) {
            passTest();
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }
        if (hasActiveTest()) {
            failTest(result.getThrowable(), BrowserManager.getPage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }
        if (hasActiveTest()) {
            String reason = result.getThrowable() != null
                    ? result.getThrowable().getMessage()
                    : "Test skipped";
            skipTest(reason);
        }
    }

    @Override
    public void onStart(ISuite suite) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        prepareChainTestOutput(suite);
        ensureServiceStarted();
        getOrCreateSuite(suite != null ? suite.getName() : null);
    }

    @Override
    public void onFinish(ISuite suite) {
        flush();
    }

    @Override
    public void onStart(ITestContext context) {
        // no-op
    }

    @Override
    public void onFinish(ITestContext context) {
        // no-op
        // Avoid duplicate JVM-level ChainTest finalization.
        // Suite-level onFinish + shutdown hook are enough.
    }


    // ============================================================
    // FILE UTILITIES
    // ============================================================

    private static Path getReportDirectory() {
        if (chainTestOutputDir != null) {
            return chainTestOutputDir;
        }

        String configuredPath = FrameworkConstants.REPORT_DIR;
        return Paths.get(configuredPath).toAbsolutePath();
    }

    private static Path getScreenshotDirectory() {
        return getReportDirectory().resolve("screenshots");
    }

    private static Path getAttachmentDirectory() {
        return getReportDirectory().resolve("attachments");
    }

    private static boolean isScreenshotOnFailureEnabled() {
        return ChainTestConfig.getBoolean(
                "chaintest.screenshot.on.failure",
                true
        );
    }

    private static String timestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    private static String sanitizeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "attachment";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
