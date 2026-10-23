package framework.utils;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.constants.FrameworkConstants;
import framework.exceptions.FrameworkException;
import framework.reporting.ReportUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageUtils {

    private static final Logger logger = LoggerFactory.getLogger(PageUtils.class);

    private PageUtils() {}

    public static void navigate(Page page, String url) {
        try {
            logger.info("Navigating to URL: {}", url);
            ReportUtils.logInfo("Navigating to URL: " + url);
            page.navigate(url);
        } catch (Exception e) {
            throw new FrameworkException("Failed to navigate to: " + url, e);
        }
    }

    public static String getTitle(Page page) {
        String title = page.title();
        logger.debug("Current page title: {}", title);
        return title;
    }

    public static String getUrl(Page page) {
        String url = page.url();
        logger.debug("Current page URL: {}", url);
        return url;
    }

    public static void refresh(Page page) {
        logger.info("Refreshing page...");
        page.reload();
    }

    public static void goBack(Page page) {
        logger.info("Navigating back...");
        page.goBack();
    }

    public static Object evaluateJs(Page page, String script) {
        logger.debug("Evaluating JS script: {}", script);
        return page.evaluate(script);
    }

    public static void highlightElement(Locator element) {
        if (!FrameworkConstants.HIGHLIGHT_WEB_ELEMENTS) {
            return;
        }

        element.evaluate(
                """
                element => {
                    // Restore previously highlighted element
                    if (window.__highlightedElement) {
                        const old = window.__highlightedElement;
    
                        old.style.outline = old.__originalOutline || '';
                        old.style.boxShadow = old.__originalBoxShadow || '';
    
                        window.__highlightedElement = null;
                    }
    
                    // Store original styles
                    element.__originalOutline = element.style.outline;
                    element.__originalBoxShadow = element.style.boxShadow;
    
                    // Scroll element into center of viewport
                    element.scrollIntoView({
                        block: 'center',
                        inline: 'center',
                        behavior: 'instant'
                    });
    
                    // Highlight
                    element.style.outline = '3px solid orange';
                    element.style.boxShadow = '0 0 10px orange';
    
                    window.__highlightedElement = element;
    
                    // Clear previous timer
                    clearTimeout(window.__highlightTimer);
    
                    // Remove highlight after 500ms
                    window.__highlightTimer = setTimeout(() => {
                        if (window.__highlightedElement === element) {
                            element.style.outline =
                                element.__originalOutline || '';
    
                            element.style.boxShadow =
                                element.__originalBoxShadow || '';
    
                            window.__highlightedElement = null;
                        }
                    }, 500);
                }
                """
        );
    }
}
