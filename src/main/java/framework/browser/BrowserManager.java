package framework.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import framework.exceptions.BrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserManager {

    private static final Logger logger = LoggerFactory.getLogger(BrowserManager.class);

    private static final ThreadLocal<BrowserContext> threadContext = new ThreadLocal<>();
    private static final ThreadLocal<Page> threadPage = new ThreadLocal<>();

    private BrowserManager() {}

    public static BrowserContext getContext() {
        BrowserContext context = threadContext.get();
        if (context == null) {
            throw new BrowserException("BrowserContext is not initialized for thread: " + Thread.currentThread().getName());
        }
        return context;
    }

    public static Page getPage() {
        Page page = threadPage.get();
        if (page == null) {
            throw new BrowserException("Page is not initialized for thread: " + Thread.currentThread().getName());
        }
        return page;
    }

    public static void setContextAndPage(BrowserContext context, Page page) {
        threadContext.set(context);
        threadPage.set(page);
        logger.debug("Set BrowserContext & Page for thread: {}", Thread.currentThread().getName());
    }

    public static void initThreadSession() {
        Browser browser = BrowserFactory.getBrowser();
        BrowserContext context = BrowserContextManager.createContext(browser);
        Page page = context.newPage();
        setContextAndPage(context, page);
    }

    public static void closeThreadSession() {
        try {
            Page page = threadPage.get();
            if (page != null && !page.isClosed()) {
                page.close();
            }
        } catch (Exception e) {
            logger.warn("Error closing Page for thread {}: {}", Thread.currentThread().getName(), e.getMessage());
        } finally {
            threadPage.remove();
        }

        try {
            BrowserContext context = threadContext.get();
            if (context != null) {
                context.close();
            }
        } catch (Exception e) {
            logger.warn("Error closing BrowserContext for thread {}: {}", Thread.currentThread().getName(), e.getMessage());
        } finally {
            threadContext.remove();
        }

        BrowserFactory.closeBrowser();
        PlaywrightManager.closePlaywright();
    }
}
