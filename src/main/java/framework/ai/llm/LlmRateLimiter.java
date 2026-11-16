/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Thread-safe rate limiter for LLM API calls, preventing quota exhaustion and enforcing configurable request throttling.
 * Created: 2026-10-22
 */
package framework.ai.llm;

import framework.ai.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe rate limiter managing concurrent LLM requests across test threads.
 */
public final class LlmRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(LlmRateLimiter.class);
    private static final Semaphore semaphore = new Semaphore(Math.max(1, AiConfig.getMaxConcurrentRequests()), true);

    private LlmRateLimiter() {}

    public static void acquire() {
        int timeoutSec = AiConfig.getTimeoutSeconds();
        try {
            boolean acquired = semaphore.tryAcquire(timeoutSec, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("Timed out waiting for LLM concurrency permit after " + timeoutSec + "s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for LLM permit", e);
        }
    }

    public static void release() {
        semaphore.release();
    }
}
