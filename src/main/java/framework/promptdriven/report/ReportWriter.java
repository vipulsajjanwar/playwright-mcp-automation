package framework.promptdriven.report;

import framework.promptdriven.exec.ExecutionContext;
import framework.promptdriven.plan.TestPlan;
import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Generates per-test JSON and rich standalone HTML reports embedding plans,
 * execution evidence, screenshots, and step timings.
 */
public class ReportWriter {

    private static final Logger logger = LoggerFactory.getLogger(ReportWriter.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static void writeReports(TestRunResult runResult, TestPlan plan, ExecutionContext context) {
        Path outputDir = context.getOutputDir();

        try {
            Files.createDirectories(outputDir);

            // 1. plan.json
            Path planPath = outputDir.resolve("plan.json");
            Files.writeString(planPath, JsonUtils.toJson(plan), StandardCharsets.UTF_8);

            // 2. report.json
            Path reportJsonPath = outputDir.resolve("report.json");
            runResult.setReportJsonPath(reportJsonPath.toString());
            runResult.setOutputDir(outputDir.toString());
            runResult.setPlanJson(JsonUtils.toJson(plan));
            Files.writeString(reportJsonPath, JsonUtils.toJson(runResult), StandardCharsets.UTF_8);

            // 3. report.html
            Path reportHtmlPath = outputDir.resolve("report.html");
            String htmlContent = generateHtmlReport(runResult, plan, context);
            Files.writeString(reportHtmlPath, htmlContent, StandardCharsets.UTF_8);
            runResult.setReportHtmlPath(reportHtmlPath.toString());

            logger.info("Successfully generated prompt test reports at: {}", outputDir);
        } catch (IOException e) {
            logger.error("Failed to write test reports to {}: {}", outputDir, e.getMessage(), e);
        }
    }

    private static String generateHtmlReport(TestRunResult run, TestPlan plan, ExecutionContext context) {
        boolean passed = run.isPassed();
        String statusColor = passed ? "#10b981" : "#ef4444";
        String statusBg = passed ? "rgba(16, 185, 129, 0.15)" : "rgba(239, 68, 68, 0.15)";
        String statusBadge = passed ? "PASSED" : "FAILED";

        StringBuilder stepsHtml = new StringBuilder();
        for (StepResult s : run.getSteps()) {
            String stepStatus = s.getStatus();
            String badgeClass = switch (stepStatus.toUpperCase()) {
                case "PASS" -> "badge-pass";
                case "HEALED" -> "badge-healed";
                default -> "badge-fail";
            };

            String screenshotImg = "";
            if (s.getScreenshotPath() != null && !s.getScreenshotPath().isBlank()) {
                Path scPath = Path.of(s.getScreenshotPath());
                if (Files.exists(scPath)) {
                    try {
                        byte[] bytes = Files.readAllBytes(scPath);
                        String base64 = Base64.getEncoder().encodeToString(bytes);
                        screenshotImg = String.format("<div class='screenshot-thumb'><a href='data:image/png;base64,%s' target='_blank'><img src='data:image/png;base64,%s' alt='Step Screenshot' /></a></div>", base64, base64);
                    } catch (Exception ignored) {}
                }
            }

            String healInfo = "";
            if (s.getOldTarget() != null && s.getNewTarget() != null) {
                healInfo = String.format("<div class='heal-box'><strong>Self-Healed:</strong> Old locator <code>%s</code> &rarr; Resilient locator <code>%s</code></div>",
                        escape(s.getOldTarget()), escape(s.getNewTarget()));
            }

            String errorBox = (s.getError() != null && !s.getError().isBlank())
                    ? String.format("<div class='error-box'>%s</div>", escape(s.getError()))
                    : "";

            String apiBox = (s.getApiDetails() != null && !s.getApiDetails().isBlank())
                    ? String.format("<div class='api-box'>%s</div>", escape(s.getApiDetails()))
                    : "";

            stepsHtml.append(String.format("""
                <tr class='%s'>
                    <td><strong>#%d</strong></td>
                    <td><span class='badge %s'>%s</span></td>
                    <td><code>%s</code></td>
                    <td>%s%s%s%s</td>
                    <td><small class='text-muted'>%s</small></td>
                    <td><small class='text-muted'>%s</small></td>
                    <td>%d ms</td>
                </tr>
                """,
                    stepStatus.toLowerCase(),
                    s.getStepId(),
                    badgeClass, stepStatus,
                    escape(s.getAction()),
                    escape(s.getSource()),
                    healInfo,
                    errorBox,
                    apiBox,
                    s.getExpected() != null ? escape(s.getExpected()) : "-",
                    s.getActual() != null ? escape(s.getActual()) : "-",
                    s.getDurationMs()
            ));
            if (!screenshotImg.isEmpty()) {
                stepsHtml.append(String.format("""
                <tr class='screenshot-row'>
                    <td colspan='7'>%s</td>
                </tr>
                """, screenshotImg));
            }
        }

        String startTimeStr = TIME_FORMATTER.format(Instant.ofEpochMilli(run.getStartTime()));

        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Prompt Test Report: %s</title>
                <style>
                    :root {
                        --bg-primary: #0f172a;
                        --bg-card: #1e293b;
                        --border: #334155;
                        --text-primary: #f8fafc;
                        --text-muted: #94a3b8;
                        --color-pass: #10b981;
                        --color-fail: #ef4444;
                        --color-healed: #f59e0b;
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background: var(--bg-primary);
                        color: var(--text-primary);
                        padding: 30px 20px;
                        line-height: 1.5;
                    }
                    .container { max-width: 1200px; margin: 0 auto; }
                    .header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        background: var(--bg-card);
                        padding: 24px 30px;
                        border-radius: 12px;
                        border: 1px solid var(--border);
                        margin-bottom: 24px;
                    }
                    .header h1 { font-size: 1.5rem; font-weight: 700; }
                    .header .meta { color: var(--text-muted); font-size: 0.875rem; margin-top: 4px; }
                    .badge-main {
                        font-size: 1.1rem;
                        font-weight: 700;
                        padding: 8px 20px;
                        border-radius: 9999px;
                        background: %s;
                        color: %s;
                        border: 1px solid %s;
                    }
                    .stats-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 16px;
                        margin-bottom: 24px;
                    }
                    .stat-card {
                        background: var(--bg-card);
                        padding: 20px;
                        border-radius: 10px;
                        border: 1px solid var(--border);
                        text-align: center;
                    }
                    .stat-card .value { font-size: 2rem; font-weight: 800; }
                    .stat-card .label { color: var(--text-muted); font-size: 0.8rem; text-transform: uppercase; margin-top: 4px; }
                    .card {
                        background: var(--bg-card);
                        border-radius: 12px;
                        border: 1px solid var(--border);
                        padding: 24px;
                        margin-bottom: 24px;
                    }
                    .card h2 { font-size: 1.25rem; margin-bottom: 16px; border-bottom: 1px solid var(--border); padding-bottom: 12px; }
                    table { width: 100%%; border-collapse: collapse; font-size: 0.9rem; }
                    th, td { padding: 12px 14px; text-align: left; border-bottom: 1px solid var(--border); vertical-align: top; }
                    th { background: rgba(0,0,0,0.2); color: var(--text-muted); font-weight: 600; text-transform: uppercase; font-size: 0.75rem; }
                    .badge {
                        display: inline-block;
                        padding: 3px 8px;
                        border-radius: 4px;
                        font-size: 0.75rem;
                        font-weight: 700;
                    }
                    .badge-pass { background: rgba(16, 185, 129, 0.2); color: var(--color-pass); }
                    .badge-fail { background: rgba(239, 68, 68, 0.2); color: var(--color-fail); }
                    .badge-healed { background: rgba(245, 158, 11, 0.2); color: var(--color-healed); }
                    .heal-box {
                        margin-top: 6px;
                        padding: 8px 12px;
                        background: rgba(245, 158, 11, 0.1);
                        border-left: 3px solid var(--color-healed);
                        border-radius: 4px;
                        font-size: 0.85rem;
                    }
                    .error-box {
                        margin-top: 6px;
                        padding: 8px 12px;
                        background: rgba(239, 68, 68, 0.1);
                        border-left: 3px solid var(--color-fail);
                        border-radius: 4px;
                        font-size: 0.85rem;
                        color: #fca5a5;
                    }
                    .api-box {
                        margin-top: 6px;
                        padding: 8px 12px;
                        background: rgba(59, 130, 246, 0.1);
                        border-left: 3px solid #3b82f6;
                        border-radius: 4px;
                        font-size: 0.85rem;
                        font-family: monospace;
                    }
                    .screenshot-thumb img {
                        max-width: 400px;
                        border-radius: 6px;
                        border: 1px solid var(--border);
                        margin-top: 8px;
                        cursor: pointer;
                        transition: transform 0.2s;
                    }
                    .screenshot-thumb img:hover { transform: scale(1.02); }
                    code { background: rgba(0,0,0,0.3); padding: 2px 6px; border-radius: 4px; font-size: 0.85rem; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div>
                            <h1>%s</h1>
                            <div class="meta">File: %s &bull; Run ID: %s &bull; Started: %s</div>
                        </div>
                        <div class="badge-main">%s</div>
                    </div>

                    <div class="stats-grid">
                        <div class="stat-card">
                            <div class="value" style="color: #60a5fa">%d</div>
                            <div class="label">Total Steps</div>
                        </div>
                        <div class="stat-card">
                            <div class="value" style="color: var(--color-pass)">%d</div>
                            <div class="label">Passed</div>
                        </div>
                        <div class="stat-card">
                            <div class="value" style="color: var(--color-fail)">%d</div>
                            <div class="label">Failed</div>
                        </div>
                        <div class="stat-card">
                            <div class="value" style="color: var(--color-healed)">%d</div>
                            <div class="label">Self-Healed</div>
                        </div>
                        <div class="stat-card">
                            <div class="value">%d ms</div>
                            <div class="label">Total Duration</div>
                        </div>
                    </div>

                    <div class="card">
                        <h2>Execution Plan &amp; Steps</h2>
                        <table>
                            <thead>
                                <tr>
                                    <th style="width: 50px;">Step</th>
                                    <th style="width: 90px;">Status</th>
                                    <th style="width: 140px;">Action</th>
                                    <th>Prompt Source / Verification Details</th>
                                    <th style="width: 180px;">Expected</th>
                                    <th style="width: 180px;">Actual</th>
                                    <th style="width: 80px;">Time</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                    </div>
                </div>
            </body>
            </html>
            """,
                escape(run.getTestName()),
                statusBg, statusColor, statusColor,
                escape(run.getTestName()),
                escape(run.getPromptName()),
                escape(run.getRunId()),
                startTimeStr,
                statusBadge,
                run.getTotalSteps(),
                run.getPassedSteps(),
                run.getFailedSteps(),
                run.getHealedSteps(),
                run.getDurationMs(),
                stepsHtml.toString()
        );
    }

    private static String escape(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
