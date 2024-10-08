package com.github.hugowschneider.cyarangodb.internal.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails.ConnectionProtocol;
import com.github.hugowschneider.cyarangodb.internal.network.ImportNetworkException;
import com.github.hugowschneider.cyarangodb.internal.test.Helper;

public class ConnectionManagerTest {

    @TempDir
    File tempDir;

    private ConnectionManager connectionManager;
    private String connectionId;

    @BeforeEach
    public void setUp() {
        connectionManager = new ConnectionManager(tempDir.getAbsolutePath());
        ConnectionDetails connectionDetails = Helper.createConnectionDetails();
        connectionId = connectionManager.addConnection(connectionDetails);
    }

    @AfterEach
    public void tearDown() {
        connectionManager.removeConnection(connectionId);
        File file = new File(connectionManager.getFilePath());
        file.delete();
    }

    @Test
    @DisplayName("ConnectionManager::addQueryToHistory")
    public void testAddQueryToHistory() {
        String query = "FOR e IN imdb_edges LIMIT 1 RETURN e";
        connectionManager.addQueryToHistory(connectionId, query);
        List<ConnectionDetails.QueryHistory> history = connectionManager.getQueryHistory(connectionId);
        assertEquals(1, history.size());
        assertEquals(query, history.get(0).getQuery());

    }

    @Test
    @DisplayName("ConnectionManager::runHistory")
    public void testRunHistory() throws ImportNetworkException {
        String query = "FOR e IN imdb_edges LIMIT 1 RETURN e";
        connectionManager.addQueryToHistory(connectionId, query);
        connectionManager.runHistory(connectionId, 0);
        List<ConnectionDetails.QueryHistory> history = connectionManager.getQueryHistory(connectionId);
        assertEquals(1, history.size());
        assertEquals(query, history.get(0).getQuery());
    }

    @Test
    @DisplayName("ConnectionManager::save and load connections")
    public void testSaveAndLoadConnections() {
        String query = "FOR e IN imdb_edges LIMIT 1 RETURN e";
        LocalDateTime dateTime = connectionManager.addQueryToHistory(connectionId, query);

        ConnectionManager newConnectionManager = new ConnectionManager(tempDir.getAbsolutePath());
        Map<String, ConnectionDetails> loadedConnections = newConnectionManager.getAllConnections();
        assertEquals(1, loadedConnections.size());
        assertEquals(connectionId, loadedConnections.keySet().iterator().next());

        List<ConnectionDetails.QueryHistory> history = newConnectionManager.getQueryHistory(connectionId);
        assertEquals(1, history.size());
        assertEquals(new ConnectionDetails.QueryHistory(query, dateTime), history.get(0));
    }

    @Test
    @DisplayName("ConnectionManager::removeConnection removes the connection correctly")
    public void testRemoveConnection() {
        connectionManager.removeConnection(connectionId);
        Map<String, ConnectionDetails> connections = connectionManager.getAllConnections();
        assertNull(connections.get(connectionId));

        List<ConnectionDetails.QueryHistory> historyAfterRemoval = connectionManager.getQueryHistory(connectionId);
        assertEquals(0, historyAfterRemoval.size());

        ConnectionManager recreatedConnectionManager = new ConnectionManager(tempDir.getAbsolutePath());
        Map<String, ConnectionDetails> loadedConnectionsAfterRemoval = recreatedConnectionManager.getAllConnections();
        assertEquals(0, loadedConnectionsAfterRemoval.size());
    }

    @Test
    @DisplayName("ConnectionManager::deleteQueryHistory removes the query history correctly")
    public void testDeleteQueryHistory() {
        String query = "FOR e IN imdb_edges LIMIT 1 RETURN e";
        connectionManager.addQueryToHistory(connectionId, query);

        connectionManager.deleteQueryHistory(connectionId, 0);
        List<ConnectionDetails.QueryHistory> historyAfterDeletion = connectionManager.getQueryHistory(connectionId);
        assertEquals(0, historyAfterDeletion.size());

        ConnectionManager recreatedConnectionManager = new ConnectionManager(tempDir.getAbsolutePath());

        List<ConnectionDetails.QueryHistory> loadedHistoryAfterDeletion = recreatedConnectionManager
                .getQueryHistory(connectionId);
        assertEquals(0, loadedHistoryAfterDeletion.size());
    }

    @Test
    @DisplayName("ConnectionManager::updateConnectionDetails updates the connection details correctly")
    public void testUpdateConnectionDetails() {

        ConnectionDetails updatedDetails = new ConnectionDetails("name_updated", "localhost_updated", 9999,
                "root_updated", "root_updated", "imdb_updated", ConnectionProtocol.HTTP2);

        connectionManager.updateConnectionDetails(connectionId, updatedDetails);

        ConnectionDetails retrievedDetails = connectionManager.getConnection(connectionId);
        assertEquals(updatedDetails.getHost(), retrievedDetails.getHost());
        assertEquals(updatedDetails.getPort(), retrievedDetails.getPort());
        assertEquals(updatedDetails.getUser(), retrievedDetails.getUser());
        assertEquals(updatedDetails.getPassword(), retrievedDetails.getPassword());
        assertEquals(updatedDetails.getDatabase(), retrievedDetails.getDatabase());
        assertEquals(updatedDetails, retrievedDetails);

        ConnectionManager recreatedConnectionManager = new ConnectionManager(tempDir.getAbsolutePath());

        ConnectionDetails reloadedDetails = recreatedConnectionManager.getConnection(connectionId);

        assertEquals(updatedDetails, reloadedDetails);

    }

}
