package com.dtas.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.dtas.driverfactory.BaseTest;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Customized  class for implement create extends report.
 * @author Jan Robert Bodino
 *         
 */

public class ExtentManager {
	 private static ExtentReports extent;
	 private static Map<Integer, ExtentTest> extentTestMap = new HashMap<>();

    public static synchronized ExtentTest getTest() {
        int threadId = (int) (long) Thread.currentThread().getId();
        return extentTestMap.get(threadId);
    }
    public static synchronized ExtentTest startTest(String testName, String description) {
        ExtentTest test = extent.createTest(testName, description);
        int threadId = (int) (long) Thread.currentThread().getId();
        extentTestMap.put(threadId, test);
        return test;
    }

    /**
     * to get report object.
     */
    public static synchronized ExtentReports getReporter() {
        if (extent == null) {
         
        	ExtentSparkReporter reporter = new ExtentSparkReporter(BaseTest.getRepoPath());
        	extent = new ExtentReports();
        	extent.attachReporter(reporter);

        }
        return extent;
    }
    
}
