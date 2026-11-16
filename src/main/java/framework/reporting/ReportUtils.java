/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Utility methods for report file management, path construction, timestamp formatting, and report artifact cleanup.
 * Created: 2026-08-15
 */
package framework.reporting;

import com.microsoft.playwright.Page;
import framework.constants.TestConstants;
import framework.context.TestContext;
import framework.context.TestContextManager;
import framework.reporting.artifacts.ScreenshotManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Utility class for reporting actions, backward-compatible with legacy tests.
 * Delegates directly to {@link FrameworkLogger} and {@link ReportingManager}
 * to prevent duplicate reporting calls.
 */
public final class ReportUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReportUtils.class);

    private ReportUtils() {}

    public static void logInfo(String message) {
        FrameworkLogger.info(message);
    }

    /**
     * Logs an HTML table into the test report.
     * The first row of the 2D array is treated as the header row.
     *
     * <p>Example usage:
     * <pre>{@code
     * ReportUtils.logTable("Validation Results", new String[][] {
     *     {"Field", "Expected", "Actual", "Status"},
     *     {"Username", "admin", "admin", "PASS"},
     *     {"Email", "a@b.com", "a@b.com", "PASS"}
     * });
     * }</pre>
     *
     * @param title a descriptive title for the table
     * @param data  2D array where data[0] is the header row and data[1..n] are body rows
     */
    public static void logTable(String title, String[][] data) {
        if (data == null || data.length == 0) {
            logInfo(title != null ? title : "Empty table");
            return;
        }
        String htmlTable = buildHtmlTable(data);
        FrameworkLogger.table(title != null ? title : "Table", htmlTable);
    }

    /**
     * Logs an HTML table into the test report with explicit headers and rows, using a default title.
     *
     * @param headers column header labels
     * @param rows    list of rows, each row being a list of cell values
     */
    public static void logTable(List<String> headers, List<List<String>> rows) {
        logTable("Table Data", headers, rows);
    }

    /**
     * Logs an HTML table into the test report with explicit headers and rows.
     *
     * <p>Example usage:
     * <pre>{@code
     * ReportUtils.logTable("API Response",
     *     List.of("Key", "Value"),
     *     List.of(
     *         List.of("Status", "200"),
     *         List.of("Body", "{...}")
     *     )
     * );
     * }</pre>
     *
     * @param title   a descriptive title for the table
     * @param headers column header labels
     * @param rows    list of rows, each row being a list of cell values
     */
    public static void logTable(String title, List<String> headers, List<List<String>> rows) {
        if (headers == null || headers.isEmpty()) {
            logInfo(title != null ? title : "Empty table");
            return;
        }
        int totalRows = 1 + (rows != null ? rows.size() : 0);
        String[][] data = new String[totalRows][];
        data[0] = headers.toArray(new String[0]);
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                data[i + 1] = row != null ? row.toArray(new String[0]) : new String[0];
            }
        }
        logTable(title, data);
    }

    /**
     * Builds a styled HTML table string from a 2D array.
     * First row is rendered as {@code <th>} header cells; remaining rows as {@code <td>} body cells.
     */
    private static String buildHtmlTable(String[][] data) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table style='border-collapse:collapse;width:100%;font-family:Arial,sans-serif;font-size:13px;'>");

        for (int i = 0; i < data.length; i++) {
            sb.append("<tr>");
            String[] row = data[i];
            if (row == null) row = new String[0];

            for (String cell : row) {
                String safeCell = cell != null ? escapeHtml(cell) : "";
                if (i == 0) {
                    // Header row
                    sb.append("<th style='border:1px solid #ddd;padding:8px 12px;background-color:#4CAF50;color:white;text-align:left;'>")
                      .append(safeCell)
                      .append("</th>");
                } else {
                    // Body row with alternating background
                    String bgColor = (i % 2 == 0) ? "#f2f2f2" : "#ffffff";
                    sb.append("<td style='border:1px solid #ddd;padding:8px 12px;background-color:")
                      .append(bgColor).append(";'>")
                      .append(safeCell)
                      .append("</td>");
                }
            }
            sb.append("</tr>");
        }

        sb.append("</table>");
        return sb.toString();
    }

    /**
     * Basic HTML entity escaping for table cell content.
     */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    public static void logPass(String message) {
        TestContext context = TestContextManager.getContext();
        if (context != null) {
            ReportingManager.getInstance().onStep(context, "[PASS] " + message);
        } else {
            logger.info("[PASS] {}", message);
        }
    }

    public static void logFail(String message, Throwable throwable) {
        TestConstants.NUMBEROFFAILURES++;
        FrameworkLogger.error(message, throwable);
    }

    public static void logFail(String message, String reason, Page page) {
        TestContext context = TestContextManager.getContext();
        TestConstants.NUMBEROFFAILURES++;
        if (page != null) {
            ScreenshotManager.captureScreenshot(page, "ValidationFailure", context);
        }
        FrameworkLogger.error(message + " - " + reason, null);
    }

    public static void logError(Exception e, String message) {
        TestConstants.NUMBEROFSKIPS++;
        FrameworkLogger.error(message, e);
    }

    public static void logScreenshot(String screenshotPath, String title) {
        TestContext context = TestContextManager.getContext();
        if (context != null) {
            context.addScreenshot(screenshotPath, null);
            ReportingManager.getInstance().onStep(context, "[SCREENSHOT] " + title + ": " + screenshotPath);
        }
    }

    public static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "unknown";
        String safe = name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        return safe.length() > 80 ? safe.substring(0, 80) : safe;
    }

    public static void reportErrorLog(Exception e, String message) {
        FrameworkLogger.error(message, e);
    }
}
