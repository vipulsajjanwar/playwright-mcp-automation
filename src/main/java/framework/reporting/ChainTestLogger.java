/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Logging bridge that routes framework log messages to the ChainTest real-time reporting pipeline.
 * Created: 2026-09-29
 */
package framework.reporting;

import framework.listeners.ChainTestListener;
import framework.utils.ChainTestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainTestLogger {

    private static final Logger logger = LoggerFactory.getLogger(ChainTestLogger.class);

    private ChainTestLogger() {
    }

    public static void info(String message) {
        log(LogLevel.INFO, message);
    }

    public static void pass(String message) {
        log(LogLevel.PASS, message);
    }

    public static void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public static void fail(String message) {
        log(LogLevel.FAIL, message);
    }

    public static void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public static void log(LogLevel level, String message) {
        if (!ChainTestUtil.isEnabled()) {
            return;
        }

        try {
            ChainTestContext context = ChainTestListener.getCurrentContext();
            if (context == null || context.getChainTestNode() == null) {
                return;
            }

            String formattedMessage = message;
            if (level != null && (message == null || !message.startsWith("[" + level.name() + "]"))) {
                formattedMessage = "[" + level.name() + "] " + (message != null ? message : "");
            }

            context.getChainTestNode().addLog(formattedMessage);
            context.incrementLogCount();
        } catch (Exception e) {
            logger.warn("Failed to write ChainTest log: {}", e.getMessage());
        }
    }
}
