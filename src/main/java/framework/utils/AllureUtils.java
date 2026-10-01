package framework.utils;

import framework.config.ConfigManager;
import io.qameta.allure.Allure;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ResultsUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Thin, static helpers around the Allure Java API.
 *
 * Dependency: io.qameta.allure:allure-java-commons (pulled in by allure-testng / allure-junit5 / allure-rest-assured).
 * Every method is safe to call outside a running test: failures are swallowed so reporting never breaks a test.
 */
public final class AllureUtils {

    private static final String RESULTS_DIR_PROP = "allure.results.directory";
    private static final String DEFAULT_RESULTS_DIR = "target/allure-results";

    /** JVM property: -Dallure.enabled=false. Env var fallback: ALLURE_ENABLED=false. Default: true. */
    public static final String ENABLED_PROP = "reporting.allure";
    public static final String ENABLED_ENV = "ALLURE_ENABLED";

    private AllureUtils() {
    }

    /**
     * The system property wins over the environment variable, which wins over the default (enabled).
     * Read on every call so it can also be flipped programmatically via System.setProperty.
     */
    public static boolean isEnabled() {
        String value = ConfigManager.get(ENABLED_PROP);
        if (value == null || value.isBlank()) {
            value = System.getenv(ENABLED_ENV);
        }
        return value == null || value.isBlank() || Boolean.parseBoolean(value.trim());
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }

    // ------------------------------------------------------------------
    // Steps
    // ------------------------------------------------------------------

    /** Runs a void step. Failure marks the step failed and rethrows the original exception. */
    public static void step(String name, Allure.ThrowableRunnableVoid body) {
        if (!isEnabled()) {
            try {
                body.run();
            } catch (Throwable t) {
                AllureUtils.<RuntimeException>sneakyThrow(t);
            }
            return;
        }
        Allure.step(name, body);
    }

    /** Runs a step that returns a value. When disabled, the body still runs; only the reporting is skipped. */
    public static <T> T stepReturning(String name, Allure.ThrowableRunnable<T> body) {
        if (!isEnabled()) {
            try {
                return body.run();
            } catch (Throwable t) {
                AllureUtils.<RuntimeException>sneakyThrow(t);
                return null; // unreachable
            }
        }
        return Allure.step(name, body);
    }

    /** Records a step that has already happened (no body), e.g. for logging a state or a soft warning. */
    public static void logStep(String name) {
        safely(() -> Allure.step(name));
    }

    /** Records a step with an explicit status (PASSED, FAILED, BROKEN, SKIPPED). Does not throw. */
    public static void logStep(String name, Status status) {
        safely(() -> {
            String uuid = UUID.randomUUID().toString();
            Allure.getLifecycle().startStep(uuid, new StepResult().setName(name).setStatus(status));
            Allure.getLifecycle().stopStep(uuid);
        });
    }

    // ------------------------------------------------------------------
    // Attachments
    // ------------------------------------------------------------------

    public static void attachText(String name, String content) {
        attach(name, "text/plain", ".txt", content);
    }

    public static void attachJson(String name, String json) {
        attach(name, "application/json", ".json", json);
    }

    public static void attachXml(String name, String xml) {
        attach(name, "application/xml", ".xml", xml);
    }

    public static void attachHtml(String name, String html) {
        attach(name, "text/html", ".html", html);
    }

    public static void attachCsv(String name, String csv) {
        attach(name, "text/csv", ".csv", csv);
    }

    /** Attach a PNG, e.g. {@code attachScreenshot("Failure", ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES))}. */
    public static void attachScreenshot(String name, byte[] png) {
        if (png == null || png.length == 0) {
            return;
        }
        safely(() -> Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), ".png"));
    }

    /** Attach an arbitrary file from disk. Content type is probed; falls back to octet-stream. */
    public static void attachFile(String name, Path file) {
        if (!isEnabled() || file == null || !Files.isRegularFile(file)) {
            return;
        }
        safely(() -> {
            String type = Files.probeContentType(file);
            String fileName = file.getFileName().toString();
            String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
            try (var in = Files.newInputStream(file)) {
                Allure.addAttachment(name, type != null ? type : "application/octet-stream", in, ext);
            }
        });
    }

    /** Attach a throwable's stack trace. */
    public static void attachStackTrace(String name, Throwable t) {
        if (!isEnabled() || t == null) {
            return;
        }
        var sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        attachText(name, sw.toString());
    }

    /** Attach an HTTP exchange in one readable block. Bodies are truncated to keep the report small. */
    public static void attachHttpExchange(String method, String url, int status,
                                          String requestBody, String responseBody, long durationMs) {
        if (!isEnabled()) {
            return;
        }
        String text = "%s %s%nStatus: %d%nDuration: %d ms%n%n--- Request body ---%n%s%n%n--- Response body ---%n%s"
                .formatted(method, url, status, durationMs, truncate(requestBody), truncate(responseBody));
        attachText(method + " " + url + " -> " + status, text);
    }

    // ------------------------------------------------------------------
    // Dynamic metadata (use annotations for static values; these are for runtime values)
    // ------------------------------------------------------------------

    public static void epic(String value) {
        label(ResultsUtils.EPIC_LABEL_NAME, value);
    }

    public static void feature(String value) {
        label(ResultsUtils.FEATURE_LABEL_NAME, value);
    }

    public static void story(String value) {
        label(ResultsUtils.STORY_LABEL_NAME, value);
    }

    public static void owner(String value) {
        label(ResultsUtils.OWNER_LABEL_NAME, value);
    }

    public static void severity(SeverityLevel level) {
        label(ResultsUtils.SEVERITY_LABEL_NAME, level.value());
    }

    public static void tag(String... tags) {
        for (String t : tags) {
            label(ResultsUtils.TAG_LABEL_NAME, t);
        }
    }

    public static void label(String name, String value) {
        safely(() -> Allure.label(name, value));
    }

    public static void parameter(String name, Object value) {
        safely(() -> Allure.parameter(name, value));
    }

    public static void description(String markdown) {
        safely(() -> Allure.description(markdown));
    }

    public static void link(String name, String url) {
        safely(() -> Allure.link(name, url));
    }

    public static void issue(String name, String url) {
        safely(() -> Allure.issue(name, url));
    }

    public static void tms(String name, String url) {
        safely(() -> Allure.tms(name, url));
    }

    /** Overrides the test name shown in the report. */
    public static void testName(String name) {
        safely(() -> Allure.getLifecycle().updateTestCase(tc -> tc.setName(name)));
    }

    // ------------------------------------------------------------------
    // Run-level files (call once, e.g. in a suite/global listener at start or end)
    // ------------------------------------------------------------------

    /** Writes environment.properties so the "Environment" widget is populated. */
    public static void writeEnvironment(Map<String, String> values) {
        Properties props = new Properties();
        values.forEach((k, v) -> {
            if (k != null && v != null) {
                props.setProperty(k, v);
            }
        });
        safely(() -> {
            Path dir = resultsDir();
            Files.createDirectories(dir);
            try (OutputStream out = Files.newOutputStream(dir.resolve("environment.properties"))) {
                props.store(out, "Allure environment");
            }
        });
    }

    /** Writes categories.json (defect classification). Pass raw JSON to customise. */
    public static void writeCategories(String json) {
        safely(() -> {
            Path dir = resultsDir();
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("categories.json"), json, StandardCharsets.UTF_8);
        });
    }

    /** Sensible defaults: product defects (assertion failures) vs. test defects (everything else broken). */
    public static void writeDefaultCategories() {
        writeCategories("""
                [
                  {"name": "Product defects", "matchedStatuses": ["failed"]},
                  {"name": "Test defects", "matchedStatuses": ["broken"]},
                  {"name": "Ignored tests", "matchedStatuses": ["skipped"]}
                ]
                """);
    }

    public static Path resultsDir() {
        return Paths.get(System.getProperty(RESULTS_DIR_PROP, DEFAULT_RESULTS_DIR));
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static void attach(String name, String type, String ext, String content) {
        if (content == null) {
            return;
        }
        safely(() -> Allure.addAttachment(name, type,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), ext));
    }

    private static String truncate(String s) {
        if (s == null || s.isEmpty()) {
            return "<empty>";
        }
        int max = 20_000;
        return s.length() <= max ? s : s.substring(0, max) + "\n... [truncated " + (s.length() - max) + " chars]";
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws Exception;
    }

    private static void safely(IoAction action) {
        if (!isEnabled()) {
            return;
        }
        try {
            action.run();
        } catch (IOException | RuntimeException e) {
            System.err.println("[AllureUtils] reporting call failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[AllureUtils] unexpected failure: " + e.getMessage());
        }
    }

    public static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static void attachLog(String level, String message) {

        String color;

        switch (level.toUpperCase()) {
            case "INFO":
                color = "#2196F3";   // Blue
                break;

            case "WARN":
                color = "#FF9800";   // Orange
                break;

            case "ERROR":
                color = "#F44336";   // Red
                break;

            case "PASS":
                color = "#4CAF50";   // Green
                break;

            case "FAIL":
                color = "#D32F2F";   // Dark Red
                break;

            default:
                color = "#333333";   // Default
        }

        String html = String.format(
                "<div style=\"font-family: monospace; " +
                        "padding: 8px; margin: 4px 0; " +
                        "border-left: 4px solid %s; " +
                        "background-color: #f8f9fa;\">" +
                        "<span style=\"color:%s; font-weight:bold;\">[%s]</span> " +
                        "<span style=\"color:#333333;\">%s</span>" +
                        "</div>",
                color,
                color,
                level.toUpperCase(),
                escape(message)
        );

        attach(nameForLevel(level), "text/html", ".html", html);
    }

    private static String nameForLevel(String level) {
        return level.toUpperCase() + " LOG";
    }
}