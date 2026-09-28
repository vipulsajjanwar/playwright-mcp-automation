package framework.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.aventstack.extentreports.reporter.configuration.ViewName;
import framework.config.ConfigManager;
import framework.constants.FrameworkConstants;
import framework.constants.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReportManager {

    private static final Logger logger = LoggerFactory.getLogger(ReportManager.class);
    private static ExtentReports extentReports;
    private static final Map<Long, ExtentTest> testMap = new ConcurrentHashMap<>();

    private ReportManager() {}

    public static synchronized ExtentReports getExtentReports(String reportName) {
        if (extentReports == null) {
            String reportPath = FrameworkConstants.EXTENT_REPORT_PATH;
            File reportFile = new File(reportPath);
            if (reportFile.getParentFile() != null && !reportFile.getParentFile().exists()) {
                reportFile.getParentFile().mkdirs();
            }

            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
            sparkReporter.config().setDocumentTitle("Playwright Automation Execution Report");
            sparkReporter.config().setReportName(reportName);
            sparkReporter.config().setTheme(Theme.DARK);
            sparkReporter.viewConfigurer()
                    .viewOrder()
                    .as(new ViewName[] {
                            ViewName.DASHBOARD,
                            ViewName.TEST,
                            ViewName.AUTHOR,
                            ViewName.DEVICE,
                            ViewName.EXCEPTION,
                            ViewName.LOG
                    })
                    .apply();

            extentReports = new ExtentReports();
            extentReports.attachReporter(sparkReporter);
            extentReports.setSystemInfo("OS", System.getProperty("os.name"));
            extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
            extentReports.setSystemInfo("Environment", ConfigManager.get("env", "qa"));
            extentReports.setSystemInfo("Browser", ConfigManager.get("browser", "chromium"));
            extentReports.setSystemInfo("APP URL", ConfigManager.get("baseUrl", "http://localhost:3000"));

            logger.info("Initialized ExtentReports at: {}", reportPath);
        }
        return extentReports;
    }

    public static synchronized ExtentTest createTest(String testName, String description) {
        ExtentTest test = getExtentReports(TestConstants.SuiteName).createTest(testName, description);
        testMap.put(Thread.currentThread().getId(), test);
        return test;
    }

    public static ExtentTest getTest() {
        return testMap.get(Thread.currentThread().getId());
    }

    public static synchronized void flushReports() {
        if (extentReports != null) {
            logger.info("Flushing ExtentReports...");
            extentReports.flush();
        }
    }
}
