package framework.excel;

import framework.constants.FileConstants;
import framework.exceptions.ExcelException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExcelReader {

    private static final Logger logger = LoggerFactory.getLogger(ExcelReader.class);

    private ExcelReader() {}

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
}
