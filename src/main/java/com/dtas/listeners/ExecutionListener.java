package com.dtas.listeners;

import com.dtas.report.ExtentTestManager; // Import ExtentTestManager
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IExecutionListener;

/**
 * Customized Listener class for handling test execution.
 * @author Jan Robert Bodino
 */
public class ExecutionListener implements IExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionListener.class);

    @Override
    public void onExecutionStart() {
        LOGGER.info("TestNG execution started.");
        // Ensure ExtentReports is initialized by accessing it.
        // Calling getTest() is a simple way to trigger the static initialization
        // of ExtentReports within ExtentTestManager and ExtentManager.
        ExtentTestManager.getTest(); // This will ensure ExtentReports is set up
        LOGGER.info("ExtentReports initialization triggered at execution start.");
    }

    @Override
    public void onExecutionFinish() {
        LOGGER.info("TestNG execution finished.");
        // Now use the public static getter method to access the extent instance
        if (ExtentTestManager.getExtentReports() != null) {
            ExtentTestManager.getExtentReports().flush();
            LOGGER.info("Extent Reports flushed successfully at end of TestNG execution.");
        } else {
            LOGGER.warn("ExtentReports instance is null at execution finish. Cannot flush reports.");
        }
    }
}