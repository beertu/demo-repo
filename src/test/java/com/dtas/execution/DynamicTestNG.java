package com.dtas.execution;

import java.io.File;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import com.codoid.products.exception.FilloException;
import com.codoid.products.fillo.Recordset;
import com.dtas.commons.FunctionLibrary;
import com.dtas.commons.XLSReader;

public class DynamicTestNG {

    static Map<String, String> xmlSuiteNameToParameterMap = new HashMap<>(); // Renamed for clarity
    static Map<String, XmlSuite> suiteFiles = new HashMap<>(); // Renamed from suteFiles
    static List<XmlSuite> suites = new ArrayList<>();
    static ArrayList<String> discoveredClasses = new ArrayList<>(); // Renamed from 'classes' for clarity

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTestNG.class);

    private final XLSReader suiteReader; // Renamed 'suite' to 'suiteReader'

    public DynamicTestNG() {
        String testdataPath = FunctionLibrary.getUserHomePath() + File.separator + "RunManager.xlsx";
        LOGGER.info("Attempting to load RunManager.xlsx from: {}", testdataPath);
        File runManagerFile = new File(testdataPath);
        if (!runManagerFile.exists()) {
            LOGGER.error("RunManager.xlsx not found at: {}. Please ensure the file exists and is accessible.", testdataPath);
            throw new RuntimeException("RunManager.xlsx not found at: " + testdataPath);
        }
        this.suiteReader = new XLSReader(testdataPath); // Initialize in constructor
        getAllDirectory(); // Discover classes once when DynamicTestNG is initialized
    }


    public static void getAllClasses(String pckgname) {
        try {
            // Ensure this path matches your compiled test classes structure
            String path = "target" + File.separator + "test-classes" + File.separator + pckgname.replace('.', File.separatorChar);
            File directory = new File(path);

            if (!directory.exists()) {
                LOGGER.warn("Directory does not exist for package: {}. Skipping class discovery for this package.", pckgname);
                return;
            }
            if (!directory.isDirectory()) {
                LOGGER.warn("Path {} is not a directory. Skipping class discovery for this path.", path);
                return;
            }

            File[] files = directory.listFiles();
            if (files == null) {
                LOGGER.warn("No files found in directory: {}", path);
                return;
            }

            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = pckgname + '.' + file.getName().substring(0, file.getName().length() - 6);
                    discoveredClasses.add(className);
                    LOGGER.debug("Discovered class: {}", className);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error in getAllClasses method for package {}: {}", pckgname, e.getMessage(), e);
        }
    }

    public void getAllDirectory() {
        try {
            String path = "target" + File.separator + "test-classes";
            File rootDirectory = new File(path);

            if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
                LOGGER.error("Root test-classes directory does not exist or is not a directory: {}. Cannot discover test classes.", path);
                return;
            }

            Queue<File> queue = new LinkedList<>();
            queue.offer(rootDirectory);

            while (!queue.isEmpty()) {
                File currentDir = queue.poll();
                File[] subDirs = currentDir.listFiles(File::isDirectory);

                if (subDirs != null) {
                    for (File subDir : subDirs) {
                        String relativePath = rootDirectory.toURI().relativize(subDir.toURI()).getPath();
                        String packageName = relativePath.replace('/', '.');
                        if (packageName.endsWith(".")) {
                            packageName = packageName.substring(0, packageName.length() - 1);
                        }

                        getAllClasses(packageName);
                        queue.offer(subDir);
                    }
                }
            }
            LOGGER.info("Finished discovering classes. Total discovered: {}", discoveredClasses.size());
            if (discoveredClasses.isEmpty()) {
                LOGGER.warn("No test classes were discovered. Check 'target/test-classes' directory and package structure.");
            }

        } catch (Exception e) {
            LOGGER.error("Error in getAllDirectory(): {}", e.getMessage(), e);
        }
    }

    public String getDirectoryName(String className) {
        // className from excel is "Testcase ID", e.g., "OSO-TS-16" or "DemoTestCase1".
        // This method should return the fully qualified class name, e.g., "com.dtas.test.DemoTestCase1"
        // Adjust filtering based on how your 'Testcase ID' maps to actual class names.
        // If "OSO-TS-16" is truly your class name, then it should match directly.
        // If "OSO-TS-16" is a test method, then you'd need to find the class that contains it.
        // For now, assuming Testcase ID is either the simple class name or full class name.
        if (className == null || className.trim().isEmpty()) {
            LOGGER.warn("Attempted to get directory name for a null or empty class name.");
            return null;
        }
        return discoveredClasses.stream()
                .filter(fullName -> fullName.endsWith(className) || fullName.substring(fullName.lastIndexOf('.') + 1).equalsIgnoreCase(className))
                .findFirst()
                .orElse(null);
    }

    // Defensive read for all helper methods
    private String getFieldSafely(Recordset recordset, String fieldName, String defaultValue, String context) {
        try {
            String value = recordset.getField(fieldName);
            if (value == null || value.trim().isEmpty()) {
                LOGGER.warn("Field '{}' is empty or null for {}. Using default value: '{}'", fieldName, context, defaultValue);
                return defaultValue;
            }
            return value.trim();
        } catch (FilloException e) {
            LOGGER.error("FilloException reading field '{}' for {}. Error: {}. Using default value: '{}'", fieldName, context, e.getMessage(), defaultValue, e);
            return defaultValue;
        } catch (Exception e) {
            LOGGER.error("Unexpected error reading field '{}' for {}. Error: {}. Using default value: '{}'", fieldName, context, e.getMessage(), defaultValue, e);
            return defaultValue;
        }
    }

    public Map<String, String> getSuiteFolderLocations() throws FilloException {
        Map<String, String> folderLocMap = new HashMap<>();
        Recordset suiteSet = null;
        try {
            suiteSet = suiteReader.getTests("select * from ParallelExecution");
            while (suiteSet.next()) {
                String suiteName = getFieldSafely(suiteSet, "SuiteName", null, "ParallelExecution row");
                String folderLoc = getFieldSafely(suiteSet, "FolderLoc", "", "ParallelExecution row for SuiteName: " + suiteName);
                
                if (suiteName != null && !suiteName.isEmpty()) { // Ensure SuiteName is present
                    folderLocMap.put(suiteName, folderLoc);
                } else {
                    LOGGER.warn("Skipping row in ParallelExecution due to missing 'SuiteName'.");
                }
            }
        } finally {
            if (suiteSet != null) {
                suiteSet.close();
            }
        }
        return folderLocMap;
    }

    public Map<String, String> getSuiteThreadCounts() throws FilloException {
        Map<String, String> threadMap = new HashMap<>();
        Recordset suiteSet = null;
        try {
            suiteSet = suiteReader.getTests("select * from ParallelExecution");
            while (suiteSet.next()) {
                String suiteName = getFieldSafely(suiteSet, "SuiteName", null, "ParallelExecution row");
                String threadCount = getFieldSafely(suiteSet, "ThreadCount", "1", "ParallelExecution row for SuiteName: " + suiteName); // Default to "1"
                
                if (suiteName != null && !suiteName.isEmpty()) { // Ensure SuiteName is present
                    threadMap.put(suiteName, threadCount);
                } else {
                    LOGGER.warn("Skipping row in ParallelExecution due to missing 'SuiteName'.");
                }
            }
        } finally {
            if (suiteSet != null) {
                suiteSet.close();
            }
        }
        return threadMap;
    }

    public Map<String, String> getSuiteTestNames() throws FilloException {
        Map<String, String> suiteTestNameMap = new HashMap<>();
        Recordset suiteSet = null;
        try {
            suiteSet = suiteReader.getTests("select * from ParallelExecution");
            while (suiteSet.next()) {
                String suiteName = getFieldSafely(suiteSet, "SuiteName", null, "ParallelExecution row");
                String suiteTestName = getFieldSafely(suiteSet, "SuiteTestName", "", "ParallelExecution row for SuiteName: " + suiteName);
                
                if (suiteName != null && !suiteName.isEmpty()) { // Ensure SuiteName is present
                    suiteTestNameMap.put(suiteName, suiteTestName);
                } else {
                    LOGGER.warn("Skipping row in ParallelExecution due to missing 'SuiteName'.");
                }
            }
        } finally {
            if (suiteSet != null) {
                suiteSet.close();
            }
        }
        return suiteTestNameMap;
    }

    @SuppressWarnings("deprecation")
    @Test
    public void executeTestNg() throws FilloException {
        Map<String, String> suiteThreadCounts = getSuiteThreadCounts();
        Map<String, String> suiteFolderLocations = getSuiteFolderLocations();
        Map<String, String> suiteTestNames = getSuiteTestNames();

        String regressionSheetName = FunctionLibrary.readPropertyValue("RunConfiguration");
        LOGGER.info("Reading test cases from sheet: {}", regressionSheetName);

        Recordset recordset = null;
        try {
            recordset = suiteReader.getTests("select * from " + regressionSheetName + " where ExecutionStatus='Yes'");

            if (recordset.getCount() == 0) {
                LOGGER.warn("No test cases found in Run Manager sheet '{}' with ExecutionStatus='Yes'. Nothing to execute.", regressionSheetName);
                return; // Exit if no records
            }

            int excelRowCounter = 2; // Assuming Excel data starts from row 2 (row 1 typically headers)
            while (recordset.next()) {
                // Use getFieldSafely for all fields from the Regression sheet
                String testCaseId = getFieldSafely(recordset, "Testcase ID", null, "Regression sheet row " + excelRowCounter);
                String description = getFieldSafely(recordset, "Description", "", "Regression sheet row " + excelRowCounter + " for Testcase ID: " + testCaseId);
                String suiteName = getFieldSafely(recordset, "Suite Name", null, "Regression sheet row " + excelRowCounter + " for Testcase ID: " + testCaseId);
                String browser = getFieldSafely(recordset, "Browser", "chrome", "Regression sheet row " + excelRowCounter + " for Testcase ID: " + testCaseId);

                // Critical validation for essential fields
                if (testCaseId == null) { // getFieldSafely returns null if not found/empty and no default
                    LOGGER.warn("Skipping test case at Excel row {} due to missing 'Testcase ID'.", excelRowCounter);
                    excelRowCounter++;
                    continue;
                }
                if (suiteName == null) { // getFieldSafely returns null if not found/empty and no default
                    LOGGER.warn("Skipping test case '{}' at Excel row {} due to missing 'Suite Name'.", testCaseId, excelRowCounter);
                    excelRowCounter++;
                    continue;
                }

                XmlSuite currentXmlSuite = suiteFiles.get(suiteName);
                if (currentXmlSuite == null) {
                    LOGGER.info("Creating new XmlSuite: {}", suiteName);
                    currentXmlSuite = new XmlSuite();
                    currentXmlSuite.setName(suiteName);
                    currentXmlSuite.setParallel(XmlSuite.ParallelMode.TESTS);

                    String threadCountStr = suiteThreadCounts.getOrDefault(suiteName, "1");
                    try {
                        currentXmlSuite.setThreadCount(Integer.parseInt(threadCountStr));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Invalid ThreadCount for suite '{}' (Excel row {}): '{}'. Defaulting to 1.", suiteName, excelRowCounter, threadCountStr, e);
                        currentXmlSuite.setThreadCount(1);
                    }

                    Map<String, String> suiteParams = new HashMap<>();
                    suiteParams.put("folderpath", suiteFolderLocations.getOrDefault(suiteName, ""));
                    suiteParams.put("suiteTestName", suiteTestNames.getOrDefault(suiteName, ""));
                    currentXmlSuite.setParameters(suiteParams);
                    suiteFiles.put(suiteName, currentXmlSuite);
                    suites.add(currentXmlSuite);
                }

                XmlTest test = new XmlTest(currentXmlSuite);
                test.setName(testCaseId);
                test.addParameter("browser", browser);
                test.addParameter("Description", description);

                List<XmlClass> classesList = new ArrayList<>();
                String fullClassName = getDirectoryName(testCaseId); // This maps Testcase ID to full class name

                if (fullClassName != null) {
                    classesList.add(new XmlClass(fullClassName));
                    test.setXmlClasses(classesList);
                    LOGGER.info("Added test '{}' (Excel row {}) to suite '{}' with class: {}", testCaseId, excelRowCounter, suiteName, fullClassName);
                } else {
                    LOGGER.error("Test class not found in discovered classes for Testcase ID: '{}' (Excel row {}). This test will not be executed.", testCaseId, excelRowCounter);
                    excelRowCounter++; // Increment even if class not found
                    continue;
                }

                xmlSuiteNameToParameterMap.put(suiteName, suiteName); // Keep this if you need to track created suites
                excelRowCounter++; // Increment for the next row
            }
        } finally {
            if (recordset != null) {
                recordset.close(); // Ensure recordset is closed even if an error occurs
            }
        }

        if (suites.isEmpty()) {
            LOGGER.warn("No valid TestNG suites were configured. Please check your Run Manager Excel data and class discovery.");
            return;
        }

        TestNG tng = new TestNG();
        tng.setXmlSuites(suites);
        LOGGER.info("Starting TestNG execution with {} suite(s).", suites.size());
        tng.run();
        LOGGER.info("TestNG execution completed.");
    }
}