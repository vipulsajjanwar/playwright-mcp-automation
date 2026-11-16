/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Constants for file system paths used by the framework: report directories, screenshot folders, and resource locations.
 * Created: 2026-08-15
 */
package framework.constants;

public final class FileConstants {

    private FileConstants() {}

    public static final String TEST_DATA_DIR = "src/test/resources/testdata/";
    public static final String EXCEL_TEST_DATA_FILE = TEST_DATA_DIR + "testdata.xlsx";
    public static final String SCHEMAS_DIR = "src/test/resources/schemas/";
    public static final String SQL_DIR = "src/test/resources/sql/";
    public static final String JSON_DIR = "src/test/resources/json/";
}
