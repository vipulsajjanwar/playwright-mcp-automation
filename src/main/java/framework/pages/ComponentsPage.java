package framework.pages;

import com.microsoft.playwright.Page;
import framework.base.BasePage;
import framework.components.*;

public class ComponentsPage extends BasePage {

    private final Header header;
    private final Sidebar sidebar;
    private final DataTable dataTable;
    private final Dropdown dropdown;
    private final Modal modal;
    private final Pagination pagination;

    public ComponentsPage(Page page) {
        super(page);
        this.header = new Header(page);
        this.sidebar = new Sidebar(page);
        this.dataTable = new DataTable(page, "#data-table");
        this.dropdown = new Dropdown(page, "#select-country", "Country Dropdown");
        this.modal = new Modal(page, "#modal-dialog");
        this.pagination = new Pagination(page);
    }

    public Header getHeader() { return header; }
    public Sidebar getSidebar() { return sidebar; }
    public DataTable getDataTable() { return dataTable; }
    public Dropdown getDropdown() { return dropdown; }
    public Modal getModal() { return modal; }
    public Pagination getPagination() { return pagination; }
}
