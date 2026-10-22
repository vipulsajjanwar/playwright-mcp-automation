package tests.promptdriven;

import framework.promptdriven.PromptFile;
import framework.promptdriven.PromptParser;
import framework.promptdriven.PromptTestRunner;
import framework.promptdriven.report.TestRunResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.*;

public class ParallelIsolationTest {

    @Test
    public void testParallelIsolationAcrossFourThreads() throws Exception {
        // Run 5 consecutive iterations to prove zero flaky cross-test interference
        for (int iteration = 1; iteration <= 5; iteration++) {
            runParallelSuiteOnce(iteration);
        }
    }

    private void runParallelSuiteOnce(int iteration) throws Exception {
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<PromptFile> prompts = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            String promptText = String.format("""
                    # test: Parallel Test %d - Iteration %d
                    # type: api
                    # mode: soft
                    base url https://jsonplaceholder.typicode.com
                    SET_VAR var_%d value_%d_iter_%d
                    GET /posts/%d
                    verify status 200
                    """, i, iteration, i, i, iteration, i);

            prompts.add(PromptParser.parse("parallel_test_" + i + ".prompt.txt", promptText));
        }

        List<Future<TestRunResult>> futures = new ArrayList<>();
        for (PromptFile pf : prompts) {
            futures.add(executor.submit(() -> PromptTestRunner.run(pf, null, null)));
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(2, TimeUnit.MINUTES);
        Assert.assertTrue(finished, "Parallel test execution timed out on iteration " + iteration);

        Set<String> outputDirs = new HashSet<>();
        for (int i = 0; i < futures.size(); i++) {
            TestRunResult res = futures.get(i).get();
            Assert.assertEquals(res.getStatus(), "PASSED", "Parallel prompt " + (i + 1) + " failed on iteration " + iteration);
            Assert.assertNotNull(res.getOutputDir());

            // Assert distinct output folders
            Assert.assertTrue(outputDirs.add(res.getOutputDir()),
                    "Duplicate output directory detected: " + res.getOutputDir());
        }
    }
}
