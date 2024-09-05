package com.github.hugowschneider.cyarangodb.internal.network;

/**
 * Represents an exception that occurs during the import process of a network.
 */
public class ImportNetworkException extends Exception {

    /**
     * Constructs a new exception with the specified detail message.
     * @param message the detail message
     */
    public ImportNetworkException(String message) {
        super(message);
    }

}
