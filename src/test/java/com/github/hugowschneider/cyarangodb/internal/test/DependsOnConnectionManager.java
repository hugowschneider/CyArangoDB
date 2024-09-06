package com.github.hugowschneider.cyarangodb.internal.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;

public class DependsOnConnectionManager {

    File tempDir;

    protected ConnectionManager connectionManager;
    protected String connectionId;

    protected void setupConnection() {
        try {
            tempDir = Files.createTempDirectory("cyarango-test").toFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        connectionManager = new ConnectionManager(tempDir.getAbsolutePath());
        ConnectionDetails connectionDetails = Helper.createConnectionDetails();
        connectionId = connectionManager.addConnection(connectionDetails);
        System.out.println(connectionManager.getFilePath());
    }

    protected void tearConnection() {
        File file = new File(connectionManager.getFilePath());
        file.delete();
    }

}
