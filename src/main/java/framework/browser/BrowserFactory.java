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

public class BrowserFactory {

    private static final Logger logger = LoggerFactory.getLogger(BrowserFactory.class);
    private static final ThreadLocal<Browser> threadBrowser = new ThreadLocal<>();

    private BrowserFactory() {}

    public static Browser getBrowser() {
        if (threadBrowser.get() == null || !threadBrowser.get().isConnected()) {
            String browserName = ConfigManager.get(
                    "browser",
                    BrowserConstants.CHROMIUM
            );

            threadBrowser.set(createBrowserInstance(browserName));
        }
        return threadBrowser.get();
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
