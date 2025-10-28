package com.dtas.exceptions;

/**
 * Customized exception for handling exceptions related to
 *         configuration in the framework.
 * @author Jan Robert Bodino 
 */
@SuppressWarnings("serial")
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }
}
