package framework.utils;

import com.microsoft.playwright.Page;
import framework.config.ConfigManager;
import org.testng.Reporter;

public class ChainTestUtil {

    private ChainTestUtil() {
        // Utility class
    }

    // =========================================================
    // ENABLE / DISABLE
    // =========================================================

    public static boolean isEnabled() {
        return Boolean.parseBoolean(ConfigManager.get("chaintest.enabled", "false"));
    }

    // =========================================================
    // INFO
    // =========================================================

    public static void info(String message) {

        if (!isEnabled()) {
            return;
        }

        log("INFO", message);
    }

    // =========================================================
    // PASS
    // =========================================================

    public static void pass(String message) {

        if (!isEnabled()) {
            return;
        }

        log("PASS", message);
    }

    // =========================================================
    // FAIL
    // =========================================================

    public static void fail(String message) {

        if (!isEnabled()) {
            return;
        }

        log("FAIL", message);
    }

    public static void fail(
            String message,
            Throwable throwable) {

        if (!isEnabled()) {
            return;
        }

        log("FAIL", message);

        exception(
                "Exception",
                throwable
        );
    }

    // =========================================================
    // WARN
    // =========================================================

    public static void warn(String message) {

        if (!isEnabled()) {
            return;
        }

        log("WARN", message);
    }

    // =========================================================
    // DEBUG
    // =========================================================

    public static void debug(String message) {

        if (!isEnabled()) {
            return;
        }

        log("DEBUG", message);
    }

    // =========================================================
    // EXCEPTION
    // =========================================================

    public static void exception(
            String message,
            Throwable throwable) {

        if (!isEnabled()) {
            return;
        }

        if (throwable == null) {

            log("EXCEPTION", message);

            return;
        }

        log(
                "EXCEPTION",
                message
                        + " | "
                        + throwable.getClass().getSimpleName()
                        + ": "
                        + throwable.getMessage()
        );
    }

    // =========================================================
    // SCREENSHOT
    // =========================================================

    public static void screenshot(
            Page page,
            String name) {

        if (!isEnabled()) {
            return;
        }

        if (page == null) {

            warn(
                    "Screenshot skipped because Page is null"
            );

            return;
        }

        try {

            byte[] screenshot =
                    page.screenshot(
                            new Page.ScreenshotOptions()
                                    .setFullPage(true)
                    );

            attachImage(
                    name,
                    screenshot
            );

        } catch (Exception e) {

            warn(
                    "Unable to capture screenshot: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // IMAGE
    // =========================================================

    public static void attachImage(
            String name,
            byte[] imageBytes) {

        if (!isEnabled()) {
            return;
        }

        if (imageBytes == null ||
                imageBytes.length == 0) {

            warn(
                    "Image attachment skipped: " + name
            );

            return;
        }

        Reporter.log(
                "[SCREENSHOT] " + name,
                true
        );
    }

    // =========================================================
    // TEXT
    // =========================================================

    public static void attachText(
            String name,
            String content) {

        if (!isEnabled()) {
            return;
        }

        Reporter.log(
                "[TEXT] " + name + "\n" + content,
                true
        );
    }

    // =========================================================
    // JSON
    // =========================================================

    public static void attachJson(
            String name,
            String json) {

        if (!isEnabled()) {
            return;
        }

        Reporter.log(
                "[JSON] " + name + "\n" + json,
                true
        );
    }

    // =========================================================
    // HTML
    // =========================================================

    public static void attachHtml(
            String name,
            String html) {

        if (!isEnabled()) {
            return;
        }

        Reporter.log(
                "[HTML] " + name + "\n" + html,
                true
        );
    }

    // =========================================================
    // INTERNAL LOG
    // =========================================================

    private static void log(
            String level,
            String message) {

        if (!isEnabled()) {
            return;
        }

        Reporter.log(
                "[" + level + "] " + message,
                true
        );
    }
}
