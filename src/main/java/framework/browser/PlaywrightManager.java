/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Central Playwright instance manager handling initialisation, configuration, and graceful shutdown of the Playwright engine.
 * Created: 2026-08-15
 */
package framework.browser;

import com.microsoft.playwright.Playwright;
import framework.exceptions.BrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaywrightManager {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightManager.class);
    private static final ThreadLocal<Playwright> threadPlaywright = new ThreadLocal<>();

    private PlaywrightManager() {}

    public static Playwright getPlaywright() {
        if (threadPlaywright.get() == null) {
            try {
                logger.info("Initializing Playwright instance [Thread: {}]", Thread.currentThread().getName());
                threadPlaywright.set(Playwright.create());
            } catch (Exception e) {
                throw new BrowserException("Failed to initialize Playwright", e);
            }
        }
        return threadPlaywright.get();
    }

    public static void closePlaywright() {
        Playwright playwright = threadPlaywright.get();
        if (playwright != null) {
            logger.info("Closing Playwright instance [Thread: {}]", Thread.currentThread().getName());
            try {
                playwright.close();
            } catch (Exception e) {
                logger.warn("Error closing Playwright for thread: {}", Thread.currentThread().getName(), e);
            } finally {
                threadPlaywright.remove();
            }
        }
    }
}
