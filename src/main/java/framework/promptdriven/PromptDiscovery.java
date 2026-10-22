package framework.promptdriven;

import framework.ai.AiConfig;
import framework.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Discovers and filters prompt files based on directories, globs, tags, and inclusion/exclusion rules.
 */
public class PromptDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(PromptDiscovery.class);

    public static List<PromptFile> discover() {
        String dirPath = AiConfig.getPromptsDir();
        String include = ConfigManager.get("prompts.include", "");
        String exclude = ConfigManager.get("prompts.exclude", "");
        String tags = ConfigManager.get("prompts.tags", "");

        return discover(dirPath, include, exclude, tags);
    }

    public static List<PromptFile> discover(String dirPath, String includePattern, String excludePattern, String tagsFilter) {
        Path baseDir = Paths.get(dirPath);
        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            logger.warn("Prompts directory does not exist or is not a directory: {}", dirPath);
            return Collections.emptyList();
        }

        List<Path> candidateFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(baseDir)) {
            candidateFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String fn = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return fn.endsWith(".prompt.txt") || fn.endsWith(".prompt");
                    })
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to scan prompts directory: {}", dirPath, e);
            return Collections.emptyList();
        }

        List<String> requiredTags = parseFilterList(tagsFilter);
        List<String> includeList = parseFilterList(includePattern);
        List<String> excludeList = parseFilterList(excludePattern);

        List<PromptFile> result = new ArrayList<>();
        for (Path p : candidateFiles) {
            String fileName = p.getFileName().toString();

            // Include filter
            if (!includeList.isEmpty()) {
                boolean matchesInclude = includeList.stream().anyMatch(inc ->
                        fileName.equalsIgnoreCase(inc) || fileName.contains(inc) || p.toString().replace('\\', '/').contains(inc));
                if (!matchesInclude) {
                    continue;
                }
            }

            // Exclude filter
            if (!excludeList.isEmpty()) {
                boolean matchesExclude = excludeList.stream().anyMatch(exc ->
                        fileName.equalsIgnoreCase(exc) || fileName.contains(exc) || p.toString().replace('\\', '/').contains(exc));
                if (matchesExclude) {
                    continue;
                }
            }

            try {
                PromptFile promptFile = PromptParser.parse(p);

                // Tag filter
                if (!requiredTags.isEmpty()) {
                    boolean hasMatchingTag = promptFile.getTags().stream()
                            .anyMatch(t -> requiredTags.stream().anyMatch(rt -> rt.equalsIgnoreCase(t)));
                    if (!hasMatchingTag) {
                        continue;
                    }
                }

                result.add(promptFile);
            } catch (Exception e) {
                logger.error("Error parsing prompt file: {}", p, e);
            }
        }

        logger.info("Discovered {} prompt file(s) in {}", result.size(), dirPath);
        return result;
    }

    private static List<String> parseFilterList(String input) {
        if (input == null || input.isBlank()) return Collections.emptyList();
        return Arrays.stream(input.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
