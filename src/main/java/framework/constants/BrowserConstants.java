/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Constants for browser configuration: supported browser names, default viewport dimensions, and launch option keys.
 * Created: 2026-08-15
 */
package framework.constants;

public final class BrowserConstants {

    private BrowserConstants() {}

    public static final String CHROMIUM = "chromium";
    public static final String FIREFOX = "firefox";
    public static final String WEBKIT = "webkit";
    public static final String CHROME = "chrome";
    public static final String MSEDGE = "msedge";

    public static final int DEFAULT_VIEWPORT_WIDTH = 1920;
    public static final int DEFAULT_VIEWPORT_HEIGHT = 1080;
    public static final String DEFAULT_LOCALE = "en-US";
    public static final String DEFAULT_TIMEZONE = "America/New_York";

    public static final boolean LOGIN_ONCE = false;
}
