package framework.utils;

import com.microsoft.playwright.Page;
import framework.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageUtils {

    private static final Logger logger = LoggerFactory.getLogger(PageUtils.class);

    private PageUtils() {}

    public static void navigate(Page page, String url) {
        try {
            logger.info("Navigating to URL: {}", url);
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
}
