package com.dtas.commons;

import com.codoid.products.exception.FilloException;
import com.codoid.products.fillo.Connection;
import com.codoid.products.fillo.Fillo;
import com.codoid.products.fillo.Recordset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XLSReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(XLSReader.class); 
    
    private final Fillo fillo;
    private final String filePath;
    
    public XLSReader(String filePath) {
        fillo = new Fillo();
        this.filePath = filePath;
        LOGGER.info("XLSReader initialized for file: {}", filePath);
    }

    /**
     * Executes a Fillo query and returns a Recordset.
     * The connection associated with this Recordset will be closed when the Recordset itself is closed.
     * It is the caller's responsibility to close the returned Recordset.
     *
     * @param query The Fillo SQL-like query to execute.
     * @return A Recordset containing the query results.
     * @throws FilloException if there is an error executing the query.
     */
    public Recordset getTests(String query) throws FilloException {
        Connection connection = null; // Declare connection locally and initialize to null
        Recordset recordset = null;
        try {
            connection = fillo.getConnection(this.filePath);
            recordset = connection.executeQuery(query);
            LOGGER.debug("Fillo query executed successfully: {}", query);
            return recordset; // Return the recordset, connection is still open
        } catch (FilloException e) {
            LOGGER.error("FilloException occurred while executing query: {}. Error: {}", query, e.getMessage(), e);
            // Crucial: close the connection here if it was successfully opened before the error
            if (connection != null) {
                connection.close(); // Attempt to close the connection if it was opened
            }
            throw e; // Re-throw the exception so the caller knows it failed
        }
        // IMPORTANT: No finally block to close the connection here.
        // The connection stays open and is managed by the Recordset.
        // The caller MUST call recordset.close() to close the underlying connection.
    }
}