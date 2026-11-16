/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Core framework-wide constants including version identifiers, default timeouts, and global configuration keys.
 * Created: 2026-08-15
 */
package framework.constants;

import framework.config.ConfigManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class FrameworkConstants {

    private FrameworkConstants() {}

    static String timestamp = new SimpleDateFormat(ConfigManager.get("timestamp.format"))
            .format(new Date());

    public static final String DEFAULT_ENV = "qa";
    public static final String CONFIG_DIR = "src/test/resources/config/";
    public static final String DEFAULT_CONFIG_PATH = CONFIG_DIR + "config.properties";
    public static final String CHAINTEST_CONFIG_PATH = "src/test/resources/chaintest.properties";

    public static final String REPORT_DIR = "reports/"+ConfigManager.get("report.Folder", "")+"/"+TestConstants.SuiteName;
    public static final String REPORT_NAME = TestConstants.SuiteName+"_"+ timestamp +".html";
    public static final String EXTENT_REPORT_PATH = REPORT_DIR+"/"+REPORT_NAME;
    public static final String TRACE_DIR = REPORT_DIR+"/traces";
    public static final String SCREENSHOT_DIR = REPORT_DIR+"/screenshots";
    public static final String VIDEO_DIR = REPORT_DIR+"/videos";
    public static final String LOG_DIR = REPORT_DIR+"/logs";

    public static final int DEFAULT_TIMEOUT_MS = 30000;
    public static final int DEFAULT_NAVIGATION_TIMEOUT_MS = 60000;

    public static final boolean HIGHLIGHT_WEB_ELEMENTS = true;

    static {
        System.setProperty("REPORT_DIR", REPORT_DIR);

    }


}
