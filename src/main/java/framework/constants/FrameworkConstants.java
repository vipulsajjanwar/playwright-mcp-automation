package framework.constants;

public final class FrameworkConstants {

    private FrameworkConstants() {}

    public static final String DEFAULT_ENV = "qa";
    public static final String CONFIG_DIR = "src/test/resources/config/";
    public static final String DEFAULT_CONFIG_PATH = CONFIG_DIR + "config.properties";

    public static final String REPORT_DIR = "target/reports";
    public static final String EXTENT_REPORT_PATH = REPORT_DIR + "/ExtentReport.html";
    public static final String TRACE_DIR = "target/traces";
    public static final String SCREENSHOT_DIR = "target/screenshots";
    public static final String VIDEO_DIR = "target/videos";
    public static final String LOG_DIR = "target/logs";

    public static final int DEFAULT_TIMEOUT_MS = 30000;
    public static final int DEFAULT_NAVIGATION_TIMEOUT_MS = 60000;

    public static final boolean HIGHLIGHT_WEB_ELEMENTS = true;
}
