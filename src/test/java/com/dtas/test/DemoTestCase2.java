package com.dtas.test;

import org.testng.annotations.Test;

import com.dtas.commons.FunctionLibrary;
import com.dtas.report.ExtentTestManager;

// No need to import com.dtas.driverfactory.* here if AppTest extends BaseTest

public class DemoTestCase2 extends AppTest { // Extends AppTest, which should extend BaseTest

    @Test
    public void demoTest2() {
        // Log the test start to Extent Reports
        ExtentTestManager.logTestResult("INFO", "DemoTestCase2 - this test is for Playwright demonstration.");

        // Call launchUrl() - this method is inherited from BaseTest (via AppTest)
        // It will use the 'page' instance initialized by BaseTest's @BeforeMethod.
        launchUrl();
        loginPage.loginUser();
        dashboard.clickOnAddToCart1();
       
        dashboard.clickOnCart();
        
    }
}