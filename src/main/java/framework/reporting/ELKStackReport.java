package framework.reporting;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import framework.browser.PlaywrightManager;
import framework.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ELKStackReport {

    private static final Logger log = LoggerFactory.getLogger(ELKStackReport.class);
    private static APIRequestContext context;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ELKStackReport() {
        // Utility class
    }

    private static synchronized APIRequestContext getContext() {
        if (context == null) {
            String user = ConfigManager.get("elk.username", "");
            String pass = ConfigManager.get("elk.password", "");
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            if (!user.isBlank()) {
                headers.put("Authorization", "Basic " + Base64.getEncoder()
                        .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8)));
            }
            context = PlaywrightManager.getPlaywright().request().newContext(
                    new APIRequest.NewContextOptions()
                            .setBaseURL(getElasticsearchUrl())
                            .setExtraHTTPHeaders(headers));
        }
        return context;
    }

    public static void logToELKStack(String body) {
        if (!isELKEnabled()) { return;}
        try {
            APIResponse r = getContext().post(
                    "/" + ConfigManager.get("elk.index", "") + "/_doc",
                    RequestOptions.create().setData(body).setTimeout(5000));
            if (!r.ok()) {
                String t = r.text();
                log.warn("ELK logging failed: {} {}", r.status(),
                        t.substring(0, Math.min(t.length(), 500)));
            }
            log.info("ELK logging succeeded: {} {}", r.status(), body);
        } catch (Exception e) {
            log.warn("ELK logging error: {}", e.getMessage());
        }
    }


    private static boolean isELKEnabled() {
        return Boolean.parseBoolean(
                ConfigManager.get("elk.enabled", "false")
        );
    }

    private static String getElasticsearchUrl() {

        String host = trimLastCharIfSlash(
                ConfigManager.get("elk.host", "")
        );

        String port =
                ConfigManager.get("elk.port", "");

        String index =
                ConfigManager.get("elk.index", "");

        String url = "http://"+host
                + ":"
                + port
                + "/"
                + index
                + "/_doc";

        log.info("ELK URL: {}", url);
        return url;
    }

    public static String trimLastCharIfSlash(String input) {

        if (input != null && input.endsWith("/")) {
            return input.substring(
                    0,
                    input.length() - 1
            );
        }

        return input;
    }

    public static synchronized void close() {
        if (context != null) { context.dispose(); context = null; }
    }

}
