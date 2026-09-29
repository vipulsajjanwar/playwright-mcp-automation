package framework.reporting;


public class ChainTestLogger {
    private ChainTestLogger() {
    }

    public static void info(
            String message) {

        log(
                LogLevel.INFO,
                message
        );
    }

    public static void pass(
            String message) {

        log(
                LogLevel.PASS,
                message
        );
    }

    public static void warn(
            String message) {

        log(
                LogLevel.WARN,
                message
        );
    }

    public static void error(
            String message) {

        log(
                LogLevel.ERROR,
                message
        );
    }

    public static void fail(
            String message) {

        log(
                LogLevel.FAIL,
                message
        );
    }

    public static void log(
            LogLevel level,
            String message) {

        /*
         * Put the actual ChainTest API call here.
         *
         * Do NOT use:
         *
         * System.out.println(...)
         *
         * because this can cause duplicate logs.
         */
    }
}
