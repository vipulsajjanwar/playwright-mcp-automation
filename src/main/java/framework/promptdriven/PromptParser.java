/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Parser that converts YAML/Markdown prompt test files into structured PromptFile and TestPlan objects.
 * Created: 2026-10-22
 */
package framework.promptdriven;

import framework.ai.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses prompt test files into structured {@link PromptFile} objects.
 * Handles headers, multi-line steps, variable extraction, and credential safety checks.
 */
public class PromptParser {

    private static final Logger logger = LoggerFactory.getLogger(PromptParser.class);

    private static final Pattern HEADER_PATTERN = Pattern.compile("^#\\s*([a-zA-Z0-9_-]+)\\s*:\\s*(.*)$");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)\\}");
    private static final Pattern LITERAL_PASSWORD_PATTERN = Pattern.compile("(?i)password\\s+(?!\\$\\{)([\"']?[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]{4,}[\"']?)");

    public static PromptFile parse(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        return parse(filePath.getFileName().toString(), content, filePath);
    }

    public static PromptFile parse(String fileName, String content) {
        return parse(fileName, content, null);
    }

    public static PromptFile parse(String fileName, String content, Path path) {
        Map<String, String> headers = new LinkedHashMap<>();
        List<String> rawStepLines = new ArrayList<>();
        Set<String> variableNames = new LinkedHashSet<>();

        String[] lines = content.split("\\r?\\n");
        StringBuilder currentMultiLineStep = null;

        for (String line : lines) {
            String trimmed = line.trim();

            // Ignore blank lines and // comments
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                if (currentMultiLineStep != null) {
                    rawStepLines.add(currentMultiLineStep.toString().trim());
                    currentMultiLineStep = null;
                }
                continue;
            }

            // Check for header comments: # key: value
            if (trimmed.startsWith("#")) {
                if (currentMultiLineStep != null) {
                    rawStepLines.add(currentMultiLineStep.toString().trim());
                    currentMultiLineStep = null;
                }
                Matcher headerMatcher = HEADER_PATTERN.matcher(trimmed);
                if (headerMatcher.matches()) {
                    String key = headerMatcher.group(1).trim().toLowerCase(Locale.ROOT);
                    String val = headerMatcher.group(2).trim();
                    headers.put(key, val);
                }
                continue;
            }

            // Check if indented (multi-line continuation of previous step)
            boolean isIndented = line.startsWith("  ") || line.startsWith("\t");
            if (isIndented && currentMultiLineStep != null) {
                currentMultiLineStep.append(" ").append(trimmed);
                continue;
            }

            if (currentMultiLineStep != null) {
                rawStepLines.add(currentMultiLineStep.toString().trim());
            }

            // Clean leading numbers or bullets
            String cleanedLine = stripBulletOrNumber(trimmed);
            currentMultiLineStep = new StringBuilder(cleanedLine);
        }

        if (currentMultiLineStep != null) {
            rawStepLines.add(currentMultiLineStep.toString().trim());
        }

        // Extract variables and check for literal credentials
        for (String step : rawStepLines) {
            Matcher varMatcher = VARIABLE_PATTERN.matcher(step);
            while (varMatcher.find()) {
                variableNames.add(varMatcher.group(1));
            }

            checkLiteralCredentials(fileName, step);
        }

        // Parse tags
        List<String> tags = new ArrayList<>();
        if (headers.containsKey("tags")) {
            String[] split = headers.get("tags").split(",\\s*");
            for (String s : split) if (!s.isBlank()) tags.add(s.trim());
        } else if (headers.containsKey("tag")) {
            tags.add(headers.get("tag").trim());
        }

        return PromptFile.builder()
                .path(path)
                .name(fileName)
                .testName(headers.getOrDefault("test", fileName))
                .type(headers.getOrDefault("type", "ui"))
                .mode(headers.getOrDefault("mode", "soft"))
                .lock(headers.get("lock"))
                .tags(tags)
                .rawStepLines(rawStepLines)
                .headers(headers)
                .variableNames(variableNames)
                .rawContent(content)
                .build();
    }

    private static String stripBulletOrNumber(String line) {
        return line.replaceFirst("^[0-9]+[.)]\\s*", "")
                   .replaceFirst("^[-*]\\s*", "")
                   .trim();
    }

    private static void checkLiteralCredentials(String fileName, String line) {
        Matcher matcher = LITERAL_PASSWORD_PATTERN.matcher(line);
        if (matcher.find()) {
            String suspectedPass = matcher.group(1);
            String warningMsg = String.format("Potential literal credential detected in prompt '%s' line: '%s'. Suspected password: %s",
                    fileName, line, suspectedPass);
            logger.warn(warningMsg);

            if (AiConfig.isPromptsStrict()) {
                throw new IllegalArgumentException("Strict prompt mode rejected execution: " + warningMsg);
            }
        }
    }
}
