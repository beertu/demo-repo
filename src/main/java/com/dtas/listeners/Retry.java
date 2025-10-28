package com.dtas.listeners;

import java.nio.file.Paths;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import com.dtas.commons.FunctionLibrary;
import com.dtas.driverfactory.BaseTest;
import com.dtas.report.ExtentTestManager;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Customized Listener class for retrying failed test cases.
 * @author Jan Robert Bodino
 */
public class Retry implements IRetryAnalyzer {

    private int count = 0;
    private static final int maxTry = Integer.parseInt(FunctionLibrary.readPropertyValue("retry_Count"));

    @Override
    public boolean retry(ITestResult testResult) {
        if (!testResult.isSuccess()) {
            if (count < maxTry) {
                count++;
                testResult.setStatus(ITestResult.FAILURE);
                extendReportsFailOperations(testResult);
                return true;
            }
        } else {
            testResult.setStatus(ITestResult.SUCCESS);
        }
        return false;
    }

    /**
     * Logs failed test operations with screenshots.
     */
    public void extendReportsFailOperations(ITestResult testResult) {
        Object testClass = testResult.getInstance();
        Page page = ((BaseTest) testClass).getPage();

        if (page != null) {
            try {
            	byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshot.png")));

                String base64Screenshot = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(screenshotBytes);
                
                ExtentTestManager.getTest().fail("Test Failed");
                ExtentTestManager.getTest().addScreenCaptureFromBase64String(base64Screenshot);

            } catch (Exception e) {
                ExtentTestManager.getTest().fail("Failed to capture screenshot due to: " + e.getMessage());
            }
        } else {
            ExtentTestManager.getTest().fail("Page instance is null. Screenshot not captured.");
        }
    }
}
