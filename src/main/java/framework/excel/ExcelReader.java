package framework.excel;

import framework.constants.FileConstants;
import framework.exceptions.ExcelException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExcelReader {

    private static final Logger logger = LoggerFactory.getLogger(ExcelReader.class);

    public ExcelReader() {
    }


    public static FileInputStream fi;
    public static FileOutputStream fo;
    public static XSSFWorkbook wb;
    public static XSSFSheet ws;
    public static XSSFRow row;
    public static XSSFCell cell;

    public static Object[][] readSheet(String filePath, String sheetName) {
        logger.info("Reading Excel sheet [{}] from file [{}]", sheetName, filePath);
        try (InputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new ExcelException("Sheet '" + sheetName + "' not found in Excel file: " + filePath);
            }

            int rowCount = sheet.getLastRowNum();
            int colCount = sheet.getRow(0).getLastCellNum();

            Object[][] data = new Object[rowCount][colCount];
            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.getRow(i);
                for (int j = 0; j < colCount; j++) {
                    Cell cell = row != null ? row.getCell(j) : null;
                    data[i - 1][j] = formatter.formatCellValue(cell);
                }
            }
            return data;
        } catch (IOException e) {
            throw new ExcelException("Failed to read Excel file: " + filePath, e);
        }
    }

    public static Object[][] readSheet(String sheetName) {
        return readSheet(FileConstants.EXCEL_TEST_DATA_FILE, sheetName);
    }

    public String[] readDataFromExcelUsingColumnName(String columnName) {
        String xlSheetName = "Sheet1";
        String cellData = "";
        String userDetails;

        userDetails = System.getProperty("user.dir") + "/src/test/resources/testdata/TestData.xlsx";

        String[] resturDetails = new String[3];
        try {
            fi = new FileInputStream(userDetails);
            wb = new XSSFWorkbook(fi);
            ws = wb.getSheet(xlSheetName);
            String user = "";
            String pass = "";
            String temppass = "";
            row = ws.getRow(0);

            int cellcount = row.getLastCellNum();

            for (int i = 0; i < cellcount; i++) {

                cell = row.getCell(i);
                DataFormatter formatter = new DataFormatter();
                cellData = formatter.formatCellValue(cell);
                if (cellData.equalsIgnoreCase(columnName)) {
                    logger.info(cellData);

                    row = ws.getRow(1);
                    cell = row.getCell(i);
                    DataFormatter formatter2 = new DataFormatter();
                    user = formatter2.formatCellValue(cell);
                    logger.info(user);

                    row = ws.getRow(2);
                    cell = row.getCell(i);
                    DataFormatter formatter3 = new DataFormatter();
                    pass = formatter3.formatCellValue(cell);
                    //	log.info(pass);

                    row = ws.getRow(3);
                    cell = row.getCell(i);
                    DataFormatter formatter4 = new DataFormatter();
                    temppass = formatter4.formatCellValue(cell);
                    //	log.info(pass);


                }

            }
            resturDetails[0] = user;

            resturDetails[1] = pass;

            resturDetails[2] = temppass;

        } catch (Exception ex) {

            logger.error(ex.getMessage());
        }
        return resturDetails;
    }
}
