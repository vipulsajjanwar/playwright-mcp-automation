package tests.promptdriven;

import framework.promptdriven.PromptDiscovery;
import framework.promptdriven.PromptFile;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class PromptDiscoveryUnitTest {

    @Test
    public void testDiscoveryAndFilters() {
        String dir = "src/test/resources/prompts";

        // Discover all
        List<PromptFile> all = PromptDiscovery.discover(dir, "", "", "");
        Assert.assertTrue(all.size() >= 3, "Expected at least 3 sample prompt files discovered");

        // Filter by include
        List<PromptFile> loginOnly = PromptDiscovery.discover(dir, "ui_login_smoke", "", "");
        Assert.assertEquals(loginOnly.size(), 1);
        Assert.assertEquals(loginOnly.get(0).getName(), "ui_login_smoke.prompt.txt");

        // Filter by tag
        List<PromptFile> apiOnly = PromptDiscovery.discover(dir, "", "", "api");
        Assert.assertTrue(apiOnly.stream().allMatch(p -> p.getTags().contains("api")));

        // Filter by exclude
        List<PromptFile> noUi = PromptDiscovery.discover(dir, "", "ui_login_smoke", "");
        Assert.assertTrue(noUi.stream().noneMatch(p -> p.getName().contains("ui_login_smoke")));
    }
}
