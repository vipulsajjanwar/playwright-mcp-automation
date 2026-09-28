package framework.excel;

import framework.exceptions.ExcelException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    public static String zipFile(String filePath, String fileExtent, String fileLocation) {
        String zipFileName = null;
        try {
            File file = new File(filePath);
            zipFileName = fileLocation+"/"+file.getName().concat(fileExtent);
            FileOutputStream fos = new FileOutputStream(zipFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            zos.putNextEntry(new ZipEntry(file.getName()));
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
            zos.close();
        } catch (FileNotFoundException e) {
            e.getMessage();
        } catch (IOException ex) {
            ex.getMessage();
        }
        return zipFileName;

    }

    public static long getFileSizeNIO(String fileName) {

        Path path = Paths.get(fileName);
        long kilobytes = 0;
        try {
            long bytes = Files.size(path);
            kilobytes = bytes / 1024;
        } catch (IOException e) {
            e.getMessage();
        }
        return kilobytes;

    }
}
