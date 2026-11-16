/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Factory class that instantiates Playwright Browser objects for Chromium, Firefox, and WebKit based on configuration.
 * Created: 2026-08-15
 */
package framework.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import framework.config.ConfigManager;
import framework.constants.BrowserConstants;
import framework.exceptions.BrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

public class BrowserFactory {

    private static final Logger logger = LoggerFactory.getLogger(BrowserFactory.class);
    private static final ThreadLocal<Browser> threadBrowser = new ThreadLocal<>();

    private BrowserFactory() {}

    public static Browser getBrowser(String browserName) {
        if (threadBrowser.get() == null || !threadBrowser.get().isConnected()) {
            String effectiveBrowser = (browserName != null && !browserName.isBlank())
                    ? normalizeBrowser(browserName)
                    : getEffectiveBrowser();
            threadBrowser.set(createBrowserInstance(effectiveBrowser));
        }
        return threadBrowser.get();
    }
    /**
     * Backward-compatible single-browser resolution.
     * If browser is missing, preserve existing fallback behavior.
     */
    public static String getEffectiveBrowser() {
        String browserName = ConfigManager.get("browser", BrowserConstants.CHROMIUM);
        if (browserName == null || browserName.isBlank()) {
            return BrowserConstants.CHROMIUM;
        }
        return normalizeBrowser(browserName);
    }

    private static String normalizeBrowser(String browserName) {
        String value = browserName.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case BrowserConstants.CHROMIUM,
                 BrowserConstants.CHROME,
                 BrowserConstants.FIREFOX,
                 BrowserConstants.WEBKIT,
                 BrowserConstants.MSEDGE -> value;
            default -> throw new BrowserException("Unsupported browser: " + browserName);
        };
    }

    /**
     * New helper for orchestration layer:
     * - if browser is provided => run only that browser
     * - if browser is missing => run all supported browsers
     */
    public static List<String> resolveRequestedBrowsers() {
        String browserName = ConfigManager.get("browser");
        if (browserName == null || browserName.isBlank()) {
            return getSupportedBrowsers();
        }
        return List.of(normalizeBrowser(browserName));
    }

    public static List<String> getSupportedBrowsers() {
        return List.of(
                BrowserConstants.CHROMIUM,
                BrowserConstants.CHROME,
                BrowserConstants.FIREFOX,
                BrowserConstants.WEBKIT,
                BrowserConstants.MSEDGE
        );
    }



    private static Browser createBrowserInstance(String browserName) {
        Playwright playwright = PlaywrightManager.getPlaywright();

        boolean headless = ConfigManager.getBoolean("headless", true);
        double slowMo = ConfigManager.getInt("slowMo", 0);

        logger.info("Launching browser: {} [Headless: {}, SlowMo: {}ms, Thread: {}]",
                browserName, headless, slowMo, Thread.currentThread().getName());

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setArgs(List.of("--start-maximized"))
                .setSlowMo(slowMo);

        try {
            switch (browserName) {
                case BrowserConstants.FIREFOX:
                    return playwright.firefox().launch(options);
                case BrowserConstants.WEBKIT:
                    return playwright.webkit().launch(options);
                case BrowserConstants.CHROME:
                    options.setChannel("chrome");
                    return playwright.chromium().launch(options);
                case BrowserConstants.MSEDGE:
                    options.setChannel("msedge");
                    return playwright.chromium().launch(options);
                case BrowserConstants.CHROMIUM:
                default:
                    return playwright.chromium().launch(options);
            }
        } catch (Exception e) {
            throw new BrowserException("Failed to launch browser: " + browserName, e);
        }
    }

    public static void closeBrowser() {
        Browser browser = threadBrowser.get();
        if (browser != null) {
            logger.info("Closing browser instance [Thread: {}]", Thread.currentThread().getName());
            try {
                browser.close();
            } catch (Exception e) {
                logger.warn("Error closing browser for thread: {}", Thread.currentThread().getName(), e);
            } finally {
                threadBrowser.remove();
            }
        }
    }
}
