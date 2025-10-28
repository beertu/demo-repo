package com.dtas.commons;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.MissingResourceException; // Added for specific exception handling

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dtas.driverfactory.BaseTest;
import com.codoid.products.exception.FilloException;
import com.codoid.products.fillo.Connection;
import com.codoid.products.fillo.Fillo;
import com.codoid.products.fillo.Recordset;

/**
 * Handles test data operations by reading from and writing to Excel sheets.
 * This class provides functionality to manage test data stored in Excel format,
 * particularly focusing on reading test case specific data using Fillo library.
 * 
 * The class is designed to work with Excel sheets containing test data organized
 * by test case IDs. It uses Fillo query capabilities to fetch specific test data
 * based on test case identifiers.
 *
 * @author Jan Robert Bodino
 */
public class TestDataHandler {

    /** Logger instance for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataHandler.class);
    
    /** Path to the Excel file containing test data */
    private String testdataPath;
    
    /** Fillo instance for Excel operations */
    private Fillo fillo;

    /**
     * Initializes a new instance of the TestDataHandler class.
     * This constructor sets up the Fillo instance for Excel operations and retrieves
     * the path to the test data Excel sheet from global properties.
     *
     * @throws IllegalStateException if the test data Excel path is not configured
     * @throws RuntimeException if initialization fails due to missing resources or other errors
     */
    public TestDataHandler() {
        try {
            // Call BaseTest.externalSheetPath() here.
            // By the time this constructor runs (called from BaseTest's @BeforeClass),
            // BaseTest.getGlobalProperties() should have already initialized externalSheetPath.
            this.testdataPath = BaseTest.externalSheetPath();
            
            if (this.testdataPath == null || this.testdataPath.trim().isEmpty()) {
                LOGGER.error("Test data Excel path is null or empty. Please check 'external_sheet_path' in global.properties.");
                throw new IllegalStateException("Test data Excel path is not configured correctly.");
            }
            
            this.fillo = new Fillo();
            LOGGER.info("TestDataHandler initialized. Using Excel path: {}", this.testdataPath);

        } catch (MissingResourceException e) {
            LOGGER.error("Missing resource exception during TestDataHandler initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize TestDataHandler due to missing global property.", e);
        } catch (SecurityException e) {
            LOGGER.error("Security error during TestDataHandler initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize TestDataHandler due to security restrictions.", e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid argument during TestDataHandler initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize TestDataHandler due to invalid arguments.", e);
        }
    }

    /**
     * Reads test data from an Excel sheet using Fillo.
     *
     * @param testCaseId The test case ID for which data needs to be fetched.
     * @return A LinkedHashMap containing the test data.
     */
    public LinkedHashMap<String, String> readDataFromExcelSheet(String testCaseId) {
        LinkedHashMap<String, String> tempSheet = new LinkedHashMap<>();
        Connection connection = null;
        Recordset recordset = null;

        try {
            // Ensure testdataPath and fillo are initialized
            if (this.testdataPath == null || this.fillo == null) {
                LOGGER.error("TestDataHandler not properly initialized. testdataPath or fillo is null.");
                throw new IllegalStateException("TestDataHandler not initialized. Cannot read data.");
            }

            connection = fillo.getConnection(testdataPath);
            String query = "SELECT * FROM TestData WHERE TestCaseId='" + testCaseId + "'";
            LOGGER.info("Executing Fillo query: {}", query);
            recordset = connection.executeQuery(query);

            if (recordset != null && recordset.getCount() > 0) {
                while (recordset.next()) {
                    for (String colName : recordset.getFieldNames()) {
                        tempSheet.put(colName, recordset.getField(colName));
                    }
                }
                LOGGER.info("Successfully read data for test case '{}'. Found {} columns.", testCaseId, tempSheet.size());
            } else {
                LOGGER.warn("No data found for test case '{}' in the 'TestData' sheet.", testCaseId);
            }
        } catch (FilloException e) {
            LOGGER.error("Fillo error occurred when reading test data for test case '{}' from sheet '{}'. Error: {}", testCaseId, "TestData", e.getMessage(), e);
            throw new RuntimeException("Failed to read test data from Excel.", e);
        } catch (IllegalStateException | SecurityException | IllegalArgumentException e) {
            LOGGER.error("Error accessing test data for test case '{}': {}", testCaseId, e.getMessage(), e);
            throw new RuntimeException("Failed to access test data due to: " + e.getMessage(), e);
        } finally {
            try {
                if (recordset != null) {
                    recordset.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception e) {
                LOGGER.error("Error while closing Fillo connection or recordset: {}", e.getMessage(), e);
            }
        }
        return tempSheet;
    }
}