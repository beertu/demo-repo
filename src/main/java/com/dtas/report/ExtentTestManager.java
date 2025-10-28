package com.dtas.report;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.ExtentTest;
import com.microsoft.playwright.Page;
import com.aventstack.extentreports.Status;

/**
 * Manages Extent Reports test instances and their associated logging and screenshot capabilities.
 * This class provides thread-safe methods for:
 * - Creating and managing test instances
 * - Logging test steps and results
 * - Capturing and attaching screenshots
 * - Tracking test step counts
 * 
 * The class is designed to work in a multi-threaded test environment, ensuring that each
 * test thread maintains its own ExtentTest instance and step count.
 *
 * @author Jan Robert Bodino
 */
public class ExtentTestManager {

    /** Logger instance for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtentTestManager.class);

    /** Thread-safe map storing ExtentTest instances per test thread */
    private static final Map<Integer, ExtentTest> extentTestMap = new HashMap<>();

    /** Thread-safe map storing step counts per test thread */
    private static final Map<Integer, Integer> stepCountMap = new HashMap<>();

    /** ExtentReports instance managed by ExtentManager */
    private static ExtentReports extent = ExtentManager.getReporter();

    // NEW: Public static getter for the ExtentReports instance
    public static ExtentReports getExtentReports() {
        return extent;
    }

    public static synchronized ExtentTest getTest() {
        int threadId = (int) (long) Thread.currentThread().getId();
        if (!extentTestMap.containsKey(threadId)) {
            LOGGER.warn("ExtentTest instance is missing for thread {}. Initializing a new test.", threadId);
//            return startTest("Default Test", "Automatically initialized due to missing instance.");
        }
        return extentTestMap.get(threadId);
    }

    public static synchronized void endTest() {
        int threadId = (int) (long) Thread.currentThread().getId();
        ExtentTest test = extentTestMap.get(threadId);
        Integer steps = stepCountMap.get(threadId);

        if (test != null && steps != null) {
            // Log the total steps to the report as an INFO message
            test.info("Total Steps: " + steps);
            LOGGER.info("Test '{}' ended with {} steps.", test.getModel().getName(), steps);
        } else {
            LOGGER.warn("Could not log total steps for thread {}. Test or step count not found.", threadId);
        }
        // Clean up maps for the finished test thread
        extentTestMap.remove(threadId);
        stepCountMap.remove(threadId);
    }

    /**
     * Start an Extent Report test.
     */
    public static synchronized ExtentTest startTest(String testName, String desc) {
        // Ensure ExtentReports instance is available before creating a test
        if (extent == null) {
            LOGGER.warn("ExtentReports instance is null when trying to start test '{}'. Attempting to re-initialize.", testName);
            extent = ExtentManager.getReporter(); // Re-attempt initialization if null
            if (extent == null) {
                LOGGER.error("Failed to initialize ExtentReports. Cannot start test: {}", testName);
                return null; // Return null if reports cannot be initialized
            }
        }

        LOGGER.info("Starting new ExtentTest: {}", testName);
        ExtentTest test = extent.createTest(testName, desc);
        int threadId = (int) (long) Thread.currentThread().getId();
        extentTestMap.put(threadId, test);
        stepCountMap.put(threadId, 0); // Initialize step count for this new test
        return test;
    }

    /**
     * Logs test status and messages to Extent Reports.
     * This method handles different types of log entries and manages test assertions.
     * 
     * @param logType The type of log entry. Valid values are:
     *                - "PASS": Logs a passing test step with hard assertion
     *                - "FAIL": Logs a failing test step with hard assertion
     *                - "INFO": Logs an informational message
     *                - "WARN": Logs a warning message
     * @param msg The message to log
     * @throws AssertionError when logType is FAIL
     */
    public static synchronized void logTestResult(String logType, String msg) {
        ExtentTest test = getTest();
        long threadId = Thread.currentThread().threadId(); // Using new API
        if (test == null) {
            LOGGER.error("Cannot log test result. ExtentTest instance is null for thread {}.", threadId);
            return;
        }

        // Increment step count for the current test
        stepCountMap.compute((int)threadId, (k, v) -> (v == null) ? 1 : v + 1);
        
        try {
            switch (logType.toUpperCase()) {
                case "PASS" -> {
                    test.log(Status.PASS, msg);
                    LOGGER.info("TEST PASS: {}", msg);
                    Assert.assertTrue(true, msg);
                }
                case "FAIL" -> {
                    test.log(Status.FAIL, msg);
                    LOGGER.error("TEST FAIL: {}", msg);
                    Assert.fail(msg);
                }
                case "INFO" -> {
                    test.log(Status.INFO, msg);
                    LOGGER.info("TEST INFO: {}", msg);
                }
                case "WARN" -> {
                    test.log(Status.WARNING, msg);
                    LOGGER.warn("TEST WARN: {}", msg);
                }
                default -> LOGGER.warn("Unsupported log type: {}", logType);
            }
        } catch (Exception e) {
            LOGGER.error("Error logging test result: {} - {}", logType, e.getMessage(), e);
            throw new RuntimeException("Failed to log test result", e);
        }
    }

    /**
     * Capture and attach screenshots to Extent Reports.
     */
    public static synchronized void takeScreenshot(Page page, String status, String msg) {
        if (page == null) {
            LOGGER.error("Cannot take screenshot. Playwright `Page` instance is null.");
            return;
        }

        ExtentTest test = getTest();
        if (test == null) {
            LOGGER.error("Cannot take screenshot. No active test instance found.");
            return;
        }

        try {
            byte[] screenshotBytes = page.screenshot();
            String base64Screenshot = Base64.getEncoder().encodeToString(screenshotBytes);

            // Log the message with the screenshot
            // Using MediaEntityBuilder is the modern way, test.addScreenCaptureFromBase64String is older but works.
            // For robustness, let's use MediaEntityBuilder if you have the dependency.
            // If MediaEntityBuilder is not compiling, stick to addScreenCaptureFromBase64String.
            // This assumes you have 'com.aventstack:extentreports' dependency.
            // You might need to add: import com.aventstack.extentreports.MediaEntityBuilder;
            test.log(Status.valueOf(status.toUpperCase()), msg,
                             com.aventstack.extentreports.MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());

            // If MediaEntityBuilder causes issues, revert to:
            // test.log(Status.valueOf(status.toUpperCase()), msg);
            // test.addScreenCaptureFromBase64String(base64Screenshot);

            LOGGER.info("Screenshot captured and attached for status: {}", status);

        } catch (Exception e) {
            LOGGER.error("Failed to take screenshot or attach to report: {}", e.getMessage(), e);
            ExtentTestManager.logTestResult("FAIL", "Screenshot capture/attachment failed: " + e.getMessage());
        }
    }

    /**
     * Ensures ExtentTest instance is initialized before logging.
     * This method is generally called by listeners or test methods before logging to Extent.
     * With the current setup, `onTestStart` in `TestListener` should handle this for each test.
     * This method could be removed if all `startTest` calls are managed by the listener.
     */
    public static synchronized boolean verifyExtentTestInstance(String testName, String description) {
        ExtentTest test = getTest();
        if (test == null) {
            LOGGER.error("ExtentTest instance is missing for test: {}", testName);
            return false;
        }
        return true;
    }
}