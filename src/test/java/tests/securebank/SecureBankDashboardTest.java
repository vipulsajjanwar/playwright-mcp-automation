package tests.securebank;

import com.securebank.pages.AccountsPage;
import com.securebank.pages.DashboardPage;
import com.securebank.pages.SecureBankLoginPage;
import framework.annotations.CustomTest;
import framework.base.BaseTest;
import framework.constants.BrowserConstants;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@CustomTest(
        module = "Dashboard",
        feature = "Dashboard",
        category = "Regression",
        requirementId = "REQ-USER-102"
)
public class SecureBankDashboardTest extends BaseTest {

    @BeforeClass(alwaysRun = true)
    public void loginOnceForClass() {
        if(BrowserConstants.LOGIN_ONCE) new SecureBankLoginPage(page()).loginWithRole("admin");
    }

    @CustomTest(
            testId = "TC-DASH-002",
            name = "Verify Total Balance Display",
            description = "Verify that a Total balance sum matches the individual account balances",
            subFeature = "Verify Total Balance Display",
            owner = "Vipul",
            severity = "High",
            priority = "P1",
            jiraId = "SB_12345",
            storyId = "SB_STORY-002",
            tags = {"dashboard", "smoke"}
    )
    @Test(groups = {"smoke", "regression"})
    public void verifyDashboardTotalBalanceDisplay() {
        try {
            if(!BrowserConstants.LOGIN_ONCE) new SecureBankLoginPage(page()).loginWithRole("admin");

            double totalAccountBalance =  new AccountsPage(page()).calculateTotalBalance();

            new DashboardPage(page()).compareNetWorthWithExpected(totalAccountBalance);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            afterTest();
        }
    }

    @CustomTest(
            testId = "TC-DASH-003",
            name = "Verify Recent Transactions Widget",
            description = "Verify Recent transactions widget displays up to 5 latest transactions",
            subFeature = "Verify Recent Transactions Widget",
            owner = "Twiggy",
            severity = "Medium",
            priority = "P2",
            jiraId = "SB_12345",
            storyId = "SB_STORY-002",
            tags = {"dashboard", "smoke"}
    )
    @Test(groups = {"smoke", "regression"})
    public void verifyRecentTransactionWidget() {
        try {
            if(!BrowserConstants.LOGIN_ONCE) new SecureBankLoginPage(page()).loginWithRole("admin");

            new DashboardPage(page()).validateRecentTransactionsWidget();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            afterTest();
        }
    }

    @CustomTest(
            testId = "TC-DASH-004",
            name = "Verify Quick Transfer Link",
            description = "Verify Clicking quick transfer navigates to /bank/transfer",
            subFeature = "Verify Quick Transfer Link",
            owner = "Twiggy",
            severity = "Low",
            priority = "P2",
            jiraId = "SB_12345",
            storyId = "SB_STORY-002",
            tags = {"dashboard", "smoke"}
    )
    @Test(groups = {"smoke", "regression"})
    public void verifyQuickTransferLink() {
        try {
            if(!BrowserConstants.LOGIN_ONCE) new SecureBankLoginPage(page()).loginWithRole("admin");

            new DashboardPage(page()).validateLinkOnDashboard("Transfer Money", "/bank/transfer");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            afterTest();
        }
    }

    @CustomTest(
            testId = "TC-DASH-005",
            name = "Verify Quick Pay Bill Link",
            description = "Verify Clicking quick pay bill navigates to /bank/bill-pay",
            subFeature = "Verify Quick Pay Bill Link",
            owner = "Twiggy",
            severity = "Low",
            priority = "P2",
            jiraId = "SB_12345",
            storyId = "SB_STORY-002",
            tags = {"dashboard", "smoke"}
    )
    @Test(groups = {"smoke", "regression"})
    public void verifyQuickPayBill() {
        try {
            if(!BrowserConstants.LOGIN_ONCE) new SecureBankLoginPage(page()).loginWithRole("admin");

            new DashboardPage(page()).validateLinkOnDashboard("Pay a Bill", "/bank/bill-pay");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            afterTest();
        }
    }

    @CustomTest(
            testId = "TC-DASH-006",
            name = "Verify Theme Toggle",
            description = "Verify Clicking the moon/sun icon toggles dark mode",
            subFeature = "Verify Theme Toggle",
            owner = "Twiggy",
            severity = "Low",
            priority = "P2",
            jiraId = "SB_12345",
            storyId = "SB_STORY-002",
            tags = {"dashboard", "smoke"}
    )
    @Test(groups = {"smoke", "regression"})
    public void verifyThemeToggle() {
        try {
            if(!BrowserConstants.LOGIN_ONCE) new SecureBankLoginPage(page()).loginWithRole("admin");

            new DashboardPage(page()).validateThemeToggle("dark");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            afterTest();
        }
    }
}
