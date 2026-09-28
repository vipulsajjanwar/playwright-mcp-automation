package framework.constants;

public final class TestConstants {

    private TestConstants() {}

    public static final int DEFAULT_RETRY_COUNT = 1;
    public static final int DEFAULT_THREAD_COUNT = 4;
    public static final String SMOKE = "smoke";
    public static final String REGRESSION = "regression";
    public static final String API = "api";
    public static final String DATABASE = "database";
    public static final String INTEGRATION = "integration";

    public static int NUMBEROFFAILURES = 0;
    public static int NUMBEROFSKIPS = 0;
    public static int NUMBEROFPASSES = 0;
    public static int TOTALTESTCASES = 0;

    public static int testCount = 0;
    public static int passedTC=0;
    public static int failedTC=0;
    public static int skippedTC=0;

    public static String SuiteName = "";
    public static String ReportName = "";

    public static String startTime = "";
    public static String endTime = "";

    public static String BaseURL = "";

    public static String ExecutionID = "";

}
