package framework.excel;

import framework.exceptions.ExcelException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelWriter {

    private static final Logger logger = LoggerFactory.getLogger(ExcelWriter.class);

    private ExcelWriter() {}

    public static void setCellData(String filePath, String sheetName, int rowNum, int colNum, String value) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.createSheet(sheetName);
            }

            Row row = sheet.getRow(rowNum);
            if (row == null) {
                row = sheet.createRow(rowNum);
            }

            Cell cell = row.getCell(colNum);
            if (cell == null) {
                cell = row.createCell(colNum);
            }

            cell.setCellValue(value);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
                logger.info("Successfully updated cell ({}, {}) in sheet [{}]", rowNum, colNum, sheetName);
            }
        } catch (IOException e) {
            throw new ExcelException("Failed to write to Excel file: " + filePath, e);
        }
    }
}
