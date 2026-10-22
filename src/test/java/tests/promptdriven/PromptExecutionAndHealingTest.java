package tests.promptdriven;

import framework.ai.AiConfig;
import framework.ai.llm.MockLlmClient;
import framework.promptdriven.PromptFile;
import framework.promptdriven.PromptParser;
import framework.promptdriven.PromptTestRunner;
import framework.promptdriven.exec.ExecutionContext;
import framework.promptdriven.report.TestRunResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PromptExecutionAndHealingTest {

    @Test
    public void testApiPromptExecutionAndPlanCaching() {
        new framework.promptdriven.plan.PlanCache().clear();
        MockLlmClient.resetCallCount();

        PromptFile prompt = PromptParser.parse("src/test/resources/prompts/api_users_crud.prompt.txt", """
                # test: API Users CRUD Test
                # type: api
                # mode: soft
                base url https://jsonplaceholder.typicode.com
                POST /users with body {"name":"Tester","email":"test@example.com"}
                verify status 201
                GET /users/1
                verify status 200 and response has field email
                """);

        // First run - generates and caches plan
        TestRunResult run1 = PromptTestRunner.run(prompt, null, null);
        Assert.assertEquals(run1.getStatus(), "PASSED");
        int callsAfterRun1 = MockLlmClient.getCallCount();
        Assert.assertTrue(callsAfterRun1 >= 1, "Expected LLM call on first plan generation");

        // Second run - must use cached plan, 0 additional LLM calls!
        TestRunResult run2 = PromptTestRunner.run(prompt, null, null);
        Assert.assertEquals(run2.getStatus(), "PASSED");
        int callsAfterRun2 = MockLlmClient.getCallCount();
        Assert.assertEquals(callsAfterRun2, callsAfterRun1, "Rerunning unchanged prompt must make ZERO LLM calls (cached plan)");
    }

    @Test
    public void testDeliberateFailingPromptReportsFailure() {
        PromptFile failingPrompt = PromptParser.parse("failing.prompt.txt", """
                # test: Deliberate Wrong Expectation
                # type: api
                # mode: fail-fast
                base url https://jsonplaceholder.typicode.com
                GET /todos/1
                verify status 404
                """);

        Assert.assertThrows(AssertionError.class, () ->
                PromptTestRunner.run(failingPrompt, null, null)
        );
    }

    @Test
    public void testDisallowedHostBlocked() {
        ExecutionContext context = new ExecutionContext("run-sec", "secTest", Paths.get("target/test-out"), true, false);

        // Disallowed host outside allowedHosts
        Assert.assertThrows(SecurityException.class, () ->
                context.validateHostAllowed("https://malicious-external-site.com/steal")
        );

        // Allowed host (jsonplaceholder.typicode.com is in allowedHosts)
        context.validateHostAllowed("https://jsonplaceholder.typicode.com/posts");
    }

    @Test
    public void testAmbiguousPromptProducesUnresolvedAndFails() {
        PromptFile ambiguousPrompt = PromptParser.parse("ambiguous.prompt.txt", """
                # test: Ambiguous Step
                # type: ui
                do something magical that cannot be resolved
                """);

        Assert.assertThrows(Exception.class, () ->
                PromptTestRunner.run(ambiguousPrompt, null, null)
        );
    }
}
