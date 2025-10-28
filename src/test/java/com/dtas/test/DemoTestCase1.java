package com.dtas.test;

import org.testng.annotations.Test;
import com.dtas.commons.FunctionLibrary;
import com.dtas.report.ExtentTestManager;

// No need to import com.dtas.driverfactory.* here if AppTest extends BaseTest

public class DemoTestCase1 extends AppTest { // Extends AppTest, which should extend BaseTest

    @Test
    public void demoTest() {
        // Log the test start to Extent Reports
        ExtentTestManager.logTestResult("INFO", "DemoTestCase1 - this test is for Playwright demonstration.");

        // Call launchUrl() - this method is inherited from BaseTest (via AppTest)
        // It will use the 'page' instance initialized by BaseTest's @BeforeMethod.
        launchUrl();
        loginPage.loginUser();
        dashboard.clickOnAddToCart1();
        dashboard.clickOnAddToCart2();
        dashboard.clickOnCart();
        
    }
    
    public static void main(String[] args) {
        // Execute Maven clean and compile
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("mvn", "clean", "compile");
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            System.out.println("Maven clean and compile exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}