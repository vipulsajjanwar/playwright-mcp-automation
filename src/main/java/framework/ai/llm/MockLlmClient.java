package framework.ai.llm;

import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic Mock LLM client for offline execution and fast unit testing.
 * Synthesizes valid closed-action vocabulary plans from prompt step lines.
 */
public class MockLlmClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(MockLlmClient.class);
    private static volatile String cannedPlanOverride = null;
    private static final AtomicInteger callCount = new AtomicInteger(0);

    public static void setCannedPlanOverride(String planJson) {
        cannedPlanOverride = planJson;
    }

    public static void clearCannedPlanOverride() {
        cannedPlanOverride = null;
    }

    public static int getCallCount() {
        return callCount.get();
    }

    public static void resetCallCount() {
        callCount.set(0);
    }

    @Override
    public String generatePlan(String systemPrompt, String userPrompt) {
        callCount.incrementAndGet();
        logger.info("MockLlmClient generating plan for prompt (calls={})", callCount.get());

        if (cannedPlanOverride != null) {
            return cannedPlanOverride;
        }

        List<Map<String, Object>> steps = new ArrayList<>();
        AtomicInteger stepId = new AtomicInteger(1);

        boolean hasStepSection = userPrompt.contains("STEP LINES TO PLAN:");
        boolean inStepSection = !hasStepSection;

        String[] lines = userPrompt.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            if (hasStepSection) {
                if (line.startsWith("STEP LINES TO PLAN:")) {
                    inStepSection = true;
                    continue;
                }
                if (line.startsWith("CRITICAL FIX NEEDED:")) {
                    inStepSection = false;
                    continue;
                }
                if (!inStepSection) {
                    continue;
                }
            } else {
                if (line.startsWith("Test ") || line.startsWith("Base URL:") || line.startsWith("Variables:") || line.startsWith("Mode:")) {
                    continue;
                }
            }

            // Strip optional number/bullet prefixes e.g. "1. ", "- "
            line = line.replaceFirst("^[0-9]+[.)]\\s*", "").replaceFirst("^[-*]\\s*", "").trim();
            if (line.isEmpty()) continue;

            String lower = line.toLowerCase(Locale.ROOT);

            if (lower.contains("ambiguous") || lower.contains("unresolved") || lower.contains("do something magical")) {
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "UNRESOLVED",
                        "args", Map.of("reason", "Cannot unambiguously map prompt line to closed action vocabulary: " + line),
                        "source", line,
                        "critical", true
                ));
            } else if (lower.startsWith("go to ") || lower.startsWith("navigate to ")) {
                String url = line.replaceFirst("^(?i)(go to|navigate to)\\s+", "").trim();
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "NAVIGATE",
                        "args", Map.of("url", url),
                        "source", line,
                        "critical", true
                ));
            } else if (lower.startsWith("login with username ") && lower.contains(" and password ")) {
                String userPart = line.substring(line.indexOf("username ") + 9, line.indexOf(" and password ")).trim();
                String passPart = line.substring(line.indexOf("password ") + 9).trim();
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "FILL",
                        "args", Map.of("target", Map.of("role", "textbox", "name", "Username"), "value", userPart),
                        "source", line,
                        "critical", true
                ));
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "FILL",
                        "args", Map.of("target", Map.of("role", "textbox", "name", "Password"), "value", passPart),
                        "source", line,
                        "critical", true
                ));
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "CLICK",
                        "args", Map.of("target", Map.of("role", "button", "name", "Login")),
                        "source", line,
                        "critical", true
                ));
            } else if (lower.startsWith("click ")) {
                String targetName = line.substring(6).trim();
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "CLICK",
                        "args", Map.of("target", Map.of("text", targetName)),
                        "source", line,
                        "critical", true
                ));
            } else if (lower.startsWith("fill ") || lower.startsWith("type ") || lower.startsWith("enter ")) {
                String remainder = line.replaceFirst("^(?i)(fill|type|enter)\\s+", "").trim();
                String target = remainder;
                String value = "";
                if (remainder.contains(" with ")) {
                    String[] parts = remainder.split(" with ", 2);
                    target = parts[0].trim();
                    value = parts[1].trim();
                }
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "FILL",
                        "args", Map.of("target", Map.of("label", target), "value", value),
                        "source", line,
                        "critical", true
                ));
            } else if (lower.startsWith("verify all ui components:") || lower.startsWith("verify components:")) {
                String itemsPart = line.substring(line.indexOf(":") + 1).trim();
                String[] items = itemsPart.split(",\\s*");
                List<Map<String, String>> compList = new ArrayList<>();
                for (String itm : items) {
                    compList.add(Map.of("name", itm.trim(), "role", "any"));
                }
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "VERIFY_COMPONENTS",
                        "args", Map.of("items", compList),
                        "source", line,
                        "critical", false
                ));
            } else if (lower.startsWith("verify table has columns")) {
                String cols = line.replaceFirst("^(?i)verify table has columns\\s*", "").trim();
                List<String> colList = Arrays.stream(cols.split(",\\s*")).map(String::trim).toList();
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "VERIFY_TABLE",
                        "args", Map.of("target", Map.of("role", "table"), "columns", colList),
                        "source", line,
                        "critical", false
                ));
            } else if (lower.contains("using generated test data") || lower.startsWith("generate data") || lower.startsWith("create a user using")) {
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "GENERATE_DATA",
                        "args", Map.of("type", "user", "count", 1, "as", "generatedUser"),
                        "source", line,
                        "critical", true
                ));
            } else if (lower.startsWith("verify status ")) {
                int status = 200;
                try {
                    String numStr = lower.replaceAll("[^0-9]", " ").trim().split("\\s+")[0];
                    status = Integer.parseInt(numStr);
                } catch (Exception ignored) {}
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "VERIFY_STATUS",
                        "args", Map.of("expected", status),
                        "source", line,
                        "critical", false
                ));
                if (lower.contains("response has field ")) {
                    String field = line.substring(line.indexOf("response has field ") + 19).trim();
                    steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "VERIFY_FIELD_EXISTS",
                        "args", Map.of("jsonPath", field),
                        "source", line,
                        "critical", false
                    ));
                }
                if (lower.contains("every item has ")) {
                    String fieldsPart = line.substring(line.indexOf("every item has ") + 15).trim();
                    List<String> fieldList = Arrays.stream(fieldsPart.split(",\\s*")).map(String::trim).toList();
                    steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "VERIFY_EVERY_ITEM_HAS",
                        "args", Map.of("arrayPath", "$", "fields", fieldList),
                        "source", line,
                        "critical", false
                    ));
                }
            } else if (lower.startsWith("base url ")) {
                String base = line.substring(9).trim();
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "API_BASE",
                        "args", Map.of("url", base),
                        "source", line,
                        "critical", true
                ));
            } else if (lower.startsWith("post ") || lower.startsWith("get ") || lower.startsWith("put ") || lower.startsWith("delete ") || lower.startsWith("patch ")) {
                String[] parts = line.split("\\s+", 3);
                String method = parts[0].toUpperCase(Locale.ROOT);
                String path = parts.length > 1 ? parts[1] : "/";
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("method", method);
                args.put("path", path);

                if (lower.contains("with body ")) {
                    String bodyStr = line.substring(line.indexOf("with body ") + 10).trim();
                    args.put("body", bodyStr);
                }
                if (lower.contains("with header ")) {
                    String headerStr = line.substring(line.indexOf("with header ") + 12).trim();
                    if (headerStr.contains(":")) {
                        String[] h = headerStr.split(":", 2);
                        args.put("headers", Map.of(h[0].trim(), h[1].trim()));
                    }
                }
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "API_REQUEST",
                        "args", args,
                        "source", line,
                        "critical", true
                ));
            } else if ((lower.startsWith("store text of ") || lower.startsWith("store value of ")) && lower.contains(" as ")) {
                // UI STORE_TEXT: "store text of <target> as <var>"
                String afterPrefix = line.replaceFirst("^(?i)store (text|value) of\\s+", "").trim();
                String targetName = afterPrefix.substring(0, afterPrefix.toLowerCase(Locale.ROOT).indexOf(" as ")).trim();
                String asVar = afterPrefix.substring(afterPrefix.toLowerCase(Locale.ROOT).indexOf(" as ") + 4).trim();
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "STORE_TEXT",
                        "args", Map.of("target", Map.of("text", targetName), "as", asVar),
                        "source", line,
                        "critical", true
                ));
            } else if (lower.startsWith("store ") && lower.contains(" as ")) {
                String path = line.substring(6, line.indexOf(" as ")).trim();
                String asVar = line.substring(line.indexOf(" as ") + 4).trim();
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "API_STORE",
                        "args", Map.of("jsonPath", path, "as", asVar),
                        "source", line,
                        "critical", true
                ));
            } else if (lower.startsWith("set_var ") || lower.startsWith("set variable ") || lower.startsWith("set var ")) {
                String remainder = line.replaceFirst("^(?i)(set_var|set variable|set var)\\s+", "").trim();
                String[] parts = remainder.split("\\s+", 2);
                String varName = parts[0].trim();
                String varVal = parts.length > 1 ? parts[1].trim() : "";
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "SET_VAR",
                        "args", Map.of("name", varName, "value", varVal),
                        "source", line,
                        "critical", true
                ));
            } else if (lower.startsWith("verify text ")) {
                // "verify text <expected>" -> VERIFY_VISIBLE with text target
                String expectedText = line.substring(12).trim();
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "VERIFY_VISIBLE",
                        "args", Map.of("target", Map.of("text", expectedText)),
                        "source", line,
                        "critical", false
                ));
            } else if (lower.matches("^verify .+ is visible$")) {
                // "verify <target> is visible" -> VERIFY_VISIBLE
                String targetName = line.substring(7, lower.lastIndexOf(" is visible")).trim();
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "VERIFY_VISIBLE",
                        "args", Map.of("target", Map.of("text", targetName)),
                        "source", line,
                        "critical", false
                ));
            } else if (lower.startsWith("verify welcome message as ") || lower.startsWith("verify message as ")) {
                // "verify welcome message as <text>" -> VERIFY_VISIBLE with text
                String expected = line.substring(line.toLowerCase(Locale.ROOT).indexOf(" as ") + 4).trim();
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "VERIFY_VISIBLE",
                        "args", Map.of("target", Map.of("text", expected)),
                        "source", line,
                        "critical", false
                ));
            } else if (lower.startsWith("verify ")) {
                String verifyTarget = line.substring(7).trim();
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "VERIFY_VISIBLE",
                        "args", Map.of("target", Map.of("text", verifyTarget)),
                        "source", line,
                        "critical", false
                ));
            } else {
                steps.add(Map.of(
                        "id", stepId.getAndIncrement(),
                        "action", "UNRESOLVED",
                        "args", Map.of("reason", "Unsupported or ambiguous prompt instruction: " + line),
                        "source", line,
                        "critical", true
                ));
            }
        }

        Map<String, Object> plan = Map.of(
                "version", "1.0",
                "steps", steps
        );
        return JsonUtils.toJson(plan);
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
