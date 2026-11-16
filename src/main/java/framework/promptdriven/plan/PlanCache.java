/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Thread-safe cache storing compiled TestPlan objects to avoid redundant parsing during parallel test execution.
 * Created: 2026-10-22
 */
package framework.promptdriven.plan;

import framework.ai.AiConfig;
import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Hash-based caching for validated test plans.
 * Avoids repeated LLM calls on unchanged prompt files unless ai.replan=true.
 */
public class PlanCache {

    private static final Logger logger = LoggerFactory.getLogger(PlanCache.class);
    private static final String DEFAULT_CACHE_DIR = "target/ai-plans";

    private final Path cacheDir;

    public PlanCache() {
        this(Paths.get(DEFAULT_CACHE_DIR));
    }

    public PlanCache(Path cacheDir) {
        this.cacheDir = cacheDir;
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            logger.warn("Could not create AI plan cache directory: {}", cacheDir, e);
        }
    }

    public static String computeHash(String promptContent) {
        if (promptContent == null) return "empty";
        String normalized = promptContent.replace("\r\n", "\n").trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public Optional<TestPlan> get(String promptHash) {
        if (AiConfig.isReplan()) {
            logger.info("Plan caching bypassed because ai.replan=true");
            return Optional.empty();
        }

        Path planPath = cacheDir.resolve(promptHash + ".json");
        if (Files.exists(planPath) && Files.isRegularFile(planPath)) {
            try {
                String json = Files.readString(planPath, StandardCharsets.UTF_8);
                TestPlan plan = JsonUtils.fromJson(json, TestPlan.class);
                logger.info("Loaded cached plan from: {}", planPath);
                return Optional.ofNullable(plan);
            } catch (Exception e) {
                logger.warn("Failed to read cached plan from {}: {}", planPath, e.getMessage());
            }
        }
        return Optional.empty();
    }

    public void put(String promptHash, TestPlan plan) {
        Path planPath = cacheDir.resolve(promptHash + ".json");
        try {
            Files.createDirectories(cacheDir);
            plan.setPromptHash(promptHash);
            String json = JsonUtils.toJson(plan);
            Files.writeString(planPath, json, StandardCharsets.UTF_8);
            logger.info("Saved validated plan to cache: {}", planPath);
        } catch (IOException e) {
            logger.error("Failed to write plan to cache: {}", planPath, e);
        }
    }

    public void clear() {
        File dir = cacheDir.toFile();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        }
    }
}
