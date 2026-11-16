/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Factory for creating and initialising TestContext instances with the correct browser, page, and metadata for each test.
 * Created: 2026-10-01
 */
package framework.context;

import framework.annotations.TestMetadata;
import framework.annotations.TestMetadataExtractor;
import framework.config.ConfigManager;
import framework.constants.TestConstants;
import org.testng.ITestResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Factory for creating initialized {@link TestContext} instances from TestNG results.
 */
public final class TestContextFactory {

    private TestContextFactory() {}

    public static TestContext createTestContext(ITestResult result) {
        TestMetadata metadata = TestMetadataExtractor.extract(result);

        String suiteName = (result != null && result.getTestContext() != null && result.getTestContext().getSuite() != null)
                ? result.getTestContext().getSuite().getName()
                : (TestConstants.SuiteName != null && !TestConstants.SuiteName.isBlank() ? TestConstants.SuiteName : "TestSuite");
        TestConstants.SuiteName=suiteName;
        String className = result != null && result.getTestClass() != null
                ? result.getTestClass().getRealClass().getSimpleName()
                : "UnknownClass";

        String methodName = result != null && result.getMethod() != null
                ? result.getMethod().getMethodName()
                : "unknownMethod";

        String[] groups = result != null && result.getMethod() != null
                ? result.getMethod().getGroups()
                : new String[0];

        String testName = metadata.getName() != null && !metadata.getName().isBlank()
                ? metadata.getName()
                : methodName;

        String executionId = (TestConstants.ExecutionID != null && !TestConstants.ExecutionID.isBlank())
                ? TestConstants.ExecutionID
                : UUID.randomUUID().toString();

        String browser = metadata.getBrowser();

        if (browser == null || browser.isBlank()) {
            if (result != null
                    && result.getTestContext() != null
                    && result.getTestContext().getCurrentXmlTest() != null) {
                browser = result.getTestContext().getCurrentXmlTest().getParameter("browser");
            }
        }

        if (browser == null || browser.isBlank()) {
            browser = TestConstants.BROWSER_NAME.get();
        }

        String environment = metadata.getEnvironment() != null && !metadata.getEnvironment().isBlank()
                ? metadata.getEnvironment()
                : ConfigManager.get("env", "qa");

        String os = System.getProperty("os.name", "Unknown OS");
        String browserVersion = System.getProperty("browser.version", "");

        // CI / Jenkins & Git environment variables
        String jenkinsJobName = getEnvOrProp("JOB_NAME", "");
        String jenkinsBuildNumber = getEnvOrProp("BUILD_NUMBER", "");
        String jenkinsBuildUrl = getEnvOrProp("BUILD_URL", "");
        String gitBranch = getEnvOrProp("GIT_BRANCH", getEnvOrProp("BRANCH_NAME", ""));
        String gitCommit = getEnvOrProp("GIT_COMMIT", "");

        long startMillis = result != null && result.getStartMillis() > 0
                ? result.getStartMillis()
                : System.currentTimeMillis();

        TestContext context = TestContext.builder()
                .executionId(executionId)
                .threadId(Thread.currentThread().getId())
                .threadName(Thread.currentThread().getName())
                .suiteName(suiteName)
                .testName(testName)
                .className(className)
                .methodName(methodName)
                .groups(groups)
                .metadata(metadata)
                .browser(browser)
                .browserVersion(browserVersion)
                .os(os)
                .environment(environment)
                .jenkinsJobName(jenkinsJobName)
                .jenkinsBuildNumber(jenkinsBuildNumber)
                .jenkinsBuildUrl(jenkinsBuildUrl)
                .gitBranch(gitBranch)
                .gitCommit(gitCommit)
                .startTime(Instant.ofEpochMilli(startMillis))
                .build();

        // Populate test data from TestNG parameters if present
        if (result != null && result.getParameters() != null && result.getParameters().length > 0) {
            for (int i = 0; i < result.getParameters().length; i++) {
                Object param = result.getParameters()[i];
                context.getTestData().put("param_" + i, param != null ? param.toString() : "null");
            }
        }

        return context;
    }

    private static String getEnvOrProp(String name, String fallback) {
        String val = System.getenv(name);
        if (val != null && !val.isBlank()) {
            return val;
        }
        val = System.getProperty(name);
        if (val != null && !val.isBlank()) {
            return val;
        }
        return fallback;
    }
}
