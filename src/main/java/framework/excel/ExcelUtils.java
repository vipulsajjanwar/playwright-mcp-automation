package framework.excel;

import framework.exceptions.ExcelException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelUtils {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUtils.class);

    private ExcelUtils() {}

    public static void createDummyExcelFile(String filePath, String sheetName, String[][] data) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < data[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(data[r][c]);
                }
            }

            java.io.File file = new java.io.File(filePath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
                logger.info("Successfully created test data Excel file: {}", filePath);
            }
        } catch (IOException e) {
            throw new ExcelException("Failed to create Excel file: " + filePath, e);
        }
    }
}
