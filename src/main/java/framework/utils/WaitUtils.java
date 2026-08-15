package framework.utils;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import framework.constants.FrameworkConstants;
import framework.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

public class WaitUtils {

    private static final Logger logger = LoggerFactory.getLogger(WaitUtils.class);

    private WaitUtils() {}

    public static void waitForVisible(Locator locator, int timeoutMs) {
        try {
            logger.debug("Waiting for element to be VISIBLE (Timeout: {}ms)", timeoutMs);
            locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(timeoutMs));
        } catch (Exception e) {
            throw new FrameworkException("Element was not visible within " + timeoutMs + "ms", e);
        }
    }

    public static void waitForVisible(Locator locator) {
        waitForVisible(locator, FrameworkConstants.DEFAULT_TIMEOUT_MS);
    }

    public static void waitForHidden(Locator locator, int timeoutMs) {
        try {
            logger.debug("Waiting for element to be HIDDEN (Timeout: {}ms)", timeoutMs);
            locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(timeoutMs));
        } catch (Exception e) {
            throw new FrameworkException("Element was not hidden within " + timeoutMs + "ms", e);
        }
    }

    public static void waitForHidden(Locator locator) {
        waitForHidden(locator, FrameworkConstants.DEFAULT_TIMEOUT_MS);
    }

    public static void waitForSpinnerToDisappear(Page page, String spinnerSelector, int timeoutMs) {
        try {
            logger.info("Waiting for spinner [{}] to disappear...", spinnerSelector);
            Locator spinner = page.locator(spinnerSelector);
            if (spinner.count() > 0 && spinner.first().isVisible()) {
                spinner.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(timeoutMs));
            }
        } catch (Exception e) {
            logger.warn("Spinner [{}] did not hide within timeout {}ms or was already gone", spinnerSelector, timeoutMs);
        }
    }

    public static void waitForApiResponse(Page page, String urlSubstring, int timeoutMs) {
        try {
            logger.info("Waiting for API response matching URL substring: {}", urlSubstring);
            page.waitForResponse(response -> response.url().contains(urlSubstring), new Page.WaitForResponseOptions().setTimeout(timeoutMs), () -> {});
        } catch (Exception e) {
            throw new FrameworkException("API response for '" + urlSubstring + "' was not received within " + timeoutMs + "ms", e);
        }
    }

    public static void waitForNetworkIdle(Page page, int timeoutMs) {
        try {
            logger.info("Waiting for network idle state...");
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(timeoutMs));
        } catch (Exception e) {
            logger.warn("Network did not reach idle state within {}ms", timeoutMs);
        }
    }
}
