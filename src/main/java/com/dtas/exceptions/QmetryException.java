package com.dtas.exceptions;

/**
 *Customized exception class for handling qmetry.
 * @author Jan Robert Bodino
 *         
 */
@SuppressWarnings("serial")
public class QmetryException extends RuntimeException {

    public QmetryException(String message) {
        super(message);
    }
}