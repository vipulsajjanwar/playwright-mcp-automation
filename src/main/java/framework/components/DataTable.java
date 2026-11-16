/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Reusable Page Component encapsulating interactions with HTML data tables: row reading, column extraction, and cell assertions.
 * Created: 2026-08-15
 */
package framework.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.utils.ElementUtils;

import java.util.ArrayList;
import java.util.List;

public class DataTable {

    private final Page page;
    private final Locator tableLocator;

    public DataTable(Page page, String tableSelector) {
        this.page = page;
        this.tableLocator = page.locator(tableSelector);
    }

    public int getRowCount() {
        return tableLocator.locator("tbody tr").count();
    }

    public List<String> getRowText(int rowIndex) {
        Locator cells = tableLocator.locator("tbody tr").nth(rowIndex).locator("td");
        List<String> cellValues = new ArrayList<>();
        for (int i = 0; i < cells.count(); i++) {
            cellValues.add(cells.nth(i).innerText().trim());
        }
        return cellValues;
    }

    public String getCellValue(int rowIndex, int colIndex) {
        return tableLocator.locator("tbody tr").nth(rowIndex).locator("td").nth(colIndex).innerText().trim();
    }
}
