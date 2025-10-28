package com.dtas.exceptions;

/**
 * class for customized data sheet exceptions.
 * @author Jan Robert Bodino
 *         
 */
@SuppressWarnings("serial")
public class DataSheetException extends RuntimeException {

    public DataSheetException(String message) {
        super(message);
    }

}
