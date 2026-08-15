package framework.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import framework.config.ConfigManager;
import framework.constants.BrowserConstants;
import framework.constants.FrameworkConstants;
import framework.exceptions.BrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public class BrowserContextManager {

    private static final Logger logger = LoggerFactory.getLogger(BrowserContextManager.class);

    public static BrowserContext createContext(Browser browser) {
        try {
            Browser.NewContextOptions options = new Browser.NewContextOptions()
                    .setViewportSize(
                            ConfigManager.getInt("viewport.width", BrowserConstants.DEFAULT_VIEWPORT_WIDTH),
                            ConfigManager.getInt("viewport.height", BrowserConstants.DEFAULT_VIEWPORT_HEIGHT)
                    )
                    .setLocale(ConfigManager.get("locale", BrowserConstants.DEFAULT_LOCALE))
                    .setTimezoneId(ConfigManager.get("timezone", BrowserConstants.DEFAULT_TIMEZONE))
                    .setAcceptDownloads(true);

            boolean videoEnabled = ConfigManager.getBoolean("video.enabled", false);
            if (videoEnabled) {
                options.setRecordVideoDir(Paths.get(FrameworkConstants.VIDEO_DIR));
            }

            logger.info("Creating new isolated BrowserContext [Thread: {}]", Thread.currentThread().getName());
            return browser.newContext(options);
        } catch (Exception e) {
            throw new BrowserException("Failed to create BrowserContext", e);
        }
    }
}
