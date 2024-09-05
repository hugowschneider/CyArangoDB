package com.github.hugowschneider.cyarangodb.internal.connection;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.network.ImportNetworkException;
import com.github.hugowschneider.cyarangodb.internal.test.Helper;

public class ConnectionManagerTest {

    @TempDir
    File tempDir;

    private ConnectionManager connectionManager;
    private static final String CONNECTION_NAME = "test-connection";

    @BeforeEach
    public void setUp() {
        connectionManager = new ConnectionManager(tempDir.getAbsolutePath());
    }

    @AfterEach
    public void tearDown() {
        File file = new File(connectionManager.getFilePath());
        file.delete();
    }

    @Test
    @DisplayName("ConnectionManager::addQueryToHistory")
    public void testAddQueryToHistory() {
        ConnectionDetails connectionDetails = Helper.createConnectionDetails();
        connectionManager.addConnection(CONNECTION_NAME, connectionDetails);
        String query = "FOR e IN imdb_edges LIMIT 1 RETURN e";
        connectionManager.addQueryToHistory(CONNECTION_NAME, query);
        List<ConnectionDetails.QueryHistory> history = connectionManager.getQueryHistory(CONNECTION_NAME);
        assertEquals(1, history.size());
        assertEquals(query, history.get(0).getQuery());
    }

    @Test
    @DisplayName("ConnectionManager::runHistory")
    public void testRunHistory() throws ImportNetworkException {
        ConnectionDetails connectionDetails = Helper.createConnectionDetails();
        connectionManager.addConnection(CONNECTION_NAME, connectionDetails);
        String query = "FOR e IN imdb_edges LIMIT 1 RETURN e";
        connectionManager.addQueryToHistory(CONNECTION_NAME, query);
        connectionManager.runHistory(CONNECTION_NAME, 0);
        List<ConnectionDetails.QueryHistory> history = connectionManager.getQueryHistory(CONNECTION_NAME);
        assertEquals(1, history.size());
        assertEquals(query, history.get(0).getQuery());
    }

    @Test
    @DisplayName("ConnectionManager::save and load connections")
    public void testSaveAndLoadConnections() {
        ConnectionDetails connectionDetails = Helper.createConnectionDetails();
        connectionManager.addConnection(CONNECTION_NAME, connectionDetails);
        String query = "FOR e IN imdb_edges LIMIT 1 RETURN e";
        connectionManager.addQueryToHistory(CONNECTION_NAME, query);

        ConnectionManager newConnectionManager = new ConnectionManager(tempDir.getAbsolutePath());
        Map<String, ConnectionDetails> loadedConnections = newConnectionManager.getAllConnections();
        assertEquals(1, loadedConnections.size());
        assertEquals(connectionDetails, loadedConnections.get(CONNECTION_NAME));

        List<ConnectionDetails.QueryHistory> history = newConnectionManager.getQueryHistory(CONNECTION_NAME);
        assertEquals(1, history.size());
        assertEquals(query, history.get(0).getQuery());
    }

    @Test
    @DisplayName("ConnectionManager::removeConnection removes the connection correctly")
    public void testRemoveConnection() {
        ConnectionDetails connectionDetails = Helper.createConnectionDetails();
        connectionManager.addConnection(CONNECTION_NAME, connectionDetails);
        connectionManager.removeConnection(CONNECTION_NAME);
        Map<String, ConnectionDetails> connections = connectionManager.getAllConnections();
        assertNull(connections.get(CONNECTION_NAME));

        List<ConnectionDetails.QueryHistory> historyAfterRemoval = connectionManager.getQueryHistory(CONNECTION_NAME);
        assertEquals(0, historyAfterRemoval.size());

        ConnectionManager recreatedConnectionManager = new ConnectionManager(tempDir.getAbsolutePath());
        Map<String, ConnectionDetails> loadedConnectionsAfterRemoval = recreatedConnectionManager.getAllConnections();
        assertEquals(0, loadedConnectionsAfterRemoval.size());
    }

    @Test
    @DisplayName("ConnectionManager::deleteQueryHistory removes the query history correctly")
    public void testDeleteQueryHistory() {
        ConnectionDetails connectionDetails = Helper.createConnectionDetails();
        connectionManager.addConnection(CONNECTION_NAME, connectionDetails);
        String query = "FOR e IN imdb_edges LIMIT 1 RETURN e";
        connectionManager.addQueryToHistory(CONNECTION_NAME, query);

        connectionManager.deleteQueryHistory(CONNECTION_NAME, 0);
        List<ConnectionDetails.QueryHistory> historyAfterDeletion = connectionManager.getQueryHistory(CONNECTION_NAME);
        assertEquals(0, historyAfterDeletion.size());

        ConnectionManager recreatedConnectionManager = new ConnectionManager(tempDir.getAbsolutePath());
        List<ConnectionDetails.QueryHistory> loadedHistoryAfterDeletion = recreatedConnectionManager.getQueryHistory(CONNECTION_NAME);
        assertEquals(0, loadedHistoryAfterDeletion.size());
    }

}