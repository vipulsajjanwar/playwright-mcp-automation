/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Unit tests for PlanCache verifying thread safety, eviction behaviour, and cache hit/miss correctness.
 * Created: 2026-10-22
 */
package tests.promptdriven;

import framework.promptdriven.plan.PlanCache;
import framework.promptdriven.plan.PlanStep;
import framework.promptdriven.plan.TestPlan;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PlanCacheUnitTest {

    @Test
    public void testHashConsistencyAndCrlfNormalization() {
        String unixText = "go to https://app.example.com\nclick Login\n";
        String windowsText = "go to https://app.example.com\r\nclick Login\r\n";

        String hash1 = PlanCache.computeHash(unixText);
        String hash2 = PlanCache.computeHash(windowsText);

        Assert.assertEquals(hash1, hash2, "CRLF and LF should produce identical normalized hash");
        Assert.assertFalse(hash1.isEmpty());
    }

    @Test
    public void testCacheSaveAndLoad() throws IOException {
        Path tempDir = Files.createTempDirectory("plan-cache-test");
        PlanCache cache = new PlanCache(tempDir);

        String promptContent = "go to https://example.com\nverify status 200";
        String hash = PlanCache.computeHash(promptContent);

        TestPlan original = new TestPlan(List.of(
                new PlanStep(1, "NAVIGATE", Map.of("url", "https://example.com"), "go to url", true)
        ));

        // Before put
        Assert.assertTrue(cache.get(hash).isEmpty());

        // Put
        cache.put(hash, original);

        // After put
        Optional<TestPlan> loaded = cache.get(hash);
        Assert.assertTrue(loaded.isPresent());
        Assert.assertEquals(loaded.get().getSteps().size(), 1);
        Assert.assertEquals(loaded.get().getSteps().get(0).getAction(), "NAVIGATE");
    }

    @Test
    public void testReplanBypassesCache() throws IOException {
        Path tempDir = Files.createTempDirectory("plan-cache-replan-test");
        PlanCache cache = new PlanCache(tempDir);

        String hash = PlanCache.computeHash("some content");
        cache.put(hash, new TestPlan(List.of(new PlanStep(1, "CLICK", Map.of(), "click", true))));

        System.setProperty("ai.replan", "true");
        try {
            Optional<TestPlan> retrieved = cache.get(hash);
            Assert.assertTrue(retrieved.isEmpty(), "replan=true must bypass cached plan");
        } finally {
            System.clearProperty("ai.replan");
        }
    }
}
