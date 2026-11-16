/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Unit tests for PromptParser covering YAML syntax, variable substitution, and malformed input error handling.
 * Created: 2026-10-22
 */
package tests.promptdriven;

import framework.promptdriven.PromptFile;
import framework.promptdriven.PromptParser;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class PromptParserUnitTest {

    @Test
    public void testParseHeadersAndStepLines() {
        String content = """
                # test: Customer Checkout Smoke
                # type: ui
                # mode: fail-fast
                # lock: checkoutLock
                # tags: smoke, checkout, p1
                // This is a comment to ignore
                
                1. go to https://app.example.com
                2. login with username ${USER_ADMIN} and password ${PASS_ADMIN}
                - click Checkout
                * verify total is 100
                """;

        PromptFile prompt = PromptParser.parse("checkout.prompt.txt", content);

        Assert.assertEquals(prompt.getName(), "checkout.prompt.txt");
        Assert.assertEquals(prompt.getTestName(), "Customer Checkout Smoke");
        Assert.assertEquals(prompt.getType(), "ui");
        Assert.assertTrue(prompt.isFailFast());
        Assert.assertEquals(prompt.getLock(), "checkoutLock");
        Assert.assertEquals(prompt.getTags(), List.of("smoke", "checkout", "p1"));

        List<String> steps = prompt.getRawStepLines();
        Assert.assertEquals(steps.size(), 4);
        Assert.assertEquals(steps.get(0), "go to https://app.example.com");
        Assert.assertEquals(steps.get(1), "login with username ${USER_ADMIN} and password ${PASS_ADMIN}");
        Assert.assertEquals(steps.get(2), "click Checkout");
        Assert.assertEquals(steps.get(3), "verify total is 100");

        Assert.assertTrue(prompt.getVariableNames().contains("USER_ADMIN"));
        Assert.assertTrue(prompt.getVariableNames().contains("PASS_ADMIN"));
    }

    @Test
    public void testMultiLineIndentedSteps() {
        String content = """
                # type: api
                POST /users with body
                  {"name": "Alice",
                   "role": "QA"}
                verify status 201
                """;

        PromptFile prompt = PromptParser.parse("multiline.prompt.txt", content);
        List<String> steps = prompt.getRawStepLines();

        Assert.assertEquals(steps.size(), 2);
        Assert.assertTrue(steps.get(0).startsWith("POST /users with body"));
        Assert.assertTrue(steps.get(0).contains("\"name\": \"Alice\""));
        Assert.assertEquals(steps.get(1), "verify status 201");
    }

    @Test
    public void testStrictLiteralPasswordRejection() {
        System.setProperty("prompts.strict", "true");
        try {
            String badContent = """
                    # test: Insecure Prompt
                    login with username admin and password SuperSecretLiteral123!
                    """;

            Assert.assertThrows(IllegalArgumentException.class, () ->
                    PromptParser.parse("insecure.prompt.txt", badContent)
            );
        } finally {
            System.clearProperty("prompts.strict");
        }
    }
}
