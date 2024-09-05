
package com.github.hugowschneider.cyarangodb.internal.connection;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.Protocol;
import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.network.ImportNetworkException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * The ConnectionManager class is responsible for managing connections to
 * ArangoDB.
 * It provides methods to add, remove, and retrieve connections, as well as
 * execute queries and manage query history.
 * The connections are stored in a JSON file for persistence.
 * 
 * Usage:
 * 1. Create an instance of ConnectionManager by calling the constructor with
 * the desired configuration folder path.
 * 2. Use the addConnection method to add a new connection.
 * 3. Use the removeConnection method to remove a connection.
 * 4. Use the getConnection method to retrieve a connection by name.
 * 5. Use the getAllConnections method to retrieve all connections.
 * 6. Use the addQueryToHistory method to add a query to the history of a
 * connection.
 * 7. Use the getQueryHistory method to retrieve the query history of a
 * connection.
 * 8. Use the validate method to check if a connection is valid.
 * 9. Use the execute method to execute a query on a connection.
 * 10. Use the runHistory method to execute a query from the query history of a
 * connection.
 * 11. Use the deleteQueryHistory method to delete a query from the query
 * history of a connection.
 * 12. Use the getArangoDatabase method to get the ArangoDatabase object for a
 * connection.
 * 
 * The ConnectionManager class uses the Gson library for JSON serialization and
 * deserialization.
 * It also uses the ArangoDB Java driver for interacting with the ArangoDB
 * database.
 */
public class ConnectionManager {

    /**
     * TypeAdapter for serializing and deserializing LocalDateTime objects to and
     * from JSON.
     */
    static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        /**
         * Writes a LocalDateTime object to JSON.
         * 
         * @param jsonWriter    the JSON writer
         * @param localDateTime the LocalDateTime object
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(JsonWriter jsonWriter, LocalDateTime localDateTime) throws IOException {
            jsonWriter.value(localDateTime.format(formatter));
        }

        /**
         * Reads a LocalDateTime object from JSON.
         * 
         * @param jsonReader the JSON reader
         * @return the LocalDateTime object
         * @throws IOException if an I/O error occurs
         */
        @Override
        public LocalDateTime read(JsonReader jsonReader) throws IOException {
            return LocalDateTime.parse(jsonReader.nextString(), formatter);
        }
    }

    /**
     * The logger for the ConnectionManager class.
     */
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ConnectionManager.class);

    /**
     * The name of the JSON file used to store the connections.
     */
    private static final String ARANGODB_CONNECTION_JSON = "arangodb-connection.json";

    /**
     * The map of connection names to connection details.
     */
    private Map<String, ConnectionDetails> connections;
    /**
     * The file path of the JSON file used to store the connections.
     */
    private final String filePath;

    /**
     * The Gson object used for JSON serialization and deserialization.
     */
    private final Gson gson;

    /**
     * Constructs a new ConnectionManager with configuration folder path set to the
     * default CytoScapeConfiguration folder.
     */
    public ConnectionManager() {
        this(System.getProperty("user.home") + File.separator + "CytoscapeConfiguration");
    }

    /**
     * Constructs a new ConnectionManager with the specified configuration folder
     * path.
     * 
     * @param configFolderPath the configuration folder path
     */
    public ConnectionManager(String configFolderPath) {
        filePath = configFolderPath
                + File.separator + ARANGODB_CONNECTION_JSON;

        File configFolder = new File(configFolderPath);
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }

        connections = new HashMap<>();

        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        loadConnections();

    }

    /**
     * Gets the file path of the JSON file used to store the connections.
     * 
     * @return the file path of the JSON file
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Adds a new connection with the specified name and details.
     * 
     * @param name    the name of the connection
     * @param details the connection details
     */
    public void addConnection(String name, ConnectionDetails details) {
        connections.put(name, details);
        saveConnections();
    }

    /**
     * Removes a connection with the specified name.
     * 
     * @param name the name of the connection to remove
     */
    public void removeConnection(String name) {
        connections.remove(name);
        saveConnections();
    }

    /**
     * Gets a connection with the specified name.
     * 
     * @param name the name of the connection
     * @return the connection details
     */
    public ConnectionDetails getConnection(String name) {
        return connections.get(name);
    }

    /**
     * Gets all connections.
     * 
     * @return a map of connection names to connection details
     */
    public Map<String, ConnectionDetails> getAllConnections() {
        return new HashMap<>(connections);
    }

    /**
     * Adds a query to the history of a connection.
     * 
     * @param connectionName the name of the connection
     * @param query          the query to add to the history
     */
    public void addQueryToHistory(String connectionName, String query) {
        this.getConnection(connectionName).addQueryToHistory(query);
        this.saveConnections();

    }

    /**
     * Gets the query history of a connection.
     * 
     * @param connectionName the name of the connection
     * @return a list of query history items
     */
    public List<ConnectionDetails.QueryHistory> getQueryHistory(String connectionName) {
        ConnectionDetails connection = this.getConnection(connectionName);
        if (connection == null) {
            return Collections.emptyList();
        } else {
            return this.getConnection(connectionName).getHistory();
        }

    }

    /**
     * Validates a connection with the specified name.
     * 
     * @param name the name of the connection
     * @return true if the connection is valid, false otherwise
     */
    public boolean validate(String name) {
        return this.validate(this.getConnection(name));
    }

    /**
     * Gets the ArangoDatabase object for a connection.
     * 
     * @param connectionDetails the connection details
     * @return the ArangoDatabase object
     */
    public ArangoDatabase getArangoDatabase(ConnectionDetails connectionDetails) {
        ArangoDB arangoDB = new ArangoDB.Builder().host(connectionDetails.getHost(), connectionDetails.getPort())
                .user(connectionDetails.getUser()).password(connectionDetails.getPassword())
                .protocol(Protocol.HTTP2_VPACK).build();

        return arangoDB.db(connectionDetails.getDatabase());
    }

    /**
     * Validates a connection with the specified connection details.
     * 
     * @param connectionDetails the connection details
     * @return true if the connection is valid, false otherwise
     */
    public boolean validate(ConnectionDetails connectionDetails) {

        ArangoDatabase db = getArangoDatabase(connectionDetails);
        return validate(db);

    }

    /**
     * Runs a query from the query history of a connection.
     * 
     * @param connectionName the name of the connection
     * @param index          the index of the query in the history
     * @return a list of RawJson documents
     * @throws ImportNetworkException if an error occurs during query execution
     */
    public List<RawJson> runHistory(String connectionName, int index) throws ImportNetworkException {
        List<ConnectionDetails.QueryHistory> history = getConnection(connectionName).getHistory();
        return execute(connectionName, history.get(index).getQuery(), false);

    }

    /**
     * Deletes a query from the query history of a connection.
     * 
     * @param connectionName the name of the connection
     * @param index          the index of the query in the history
     */
    public void deleteQueryHistory(String connectionName, int index) {
        // Implementation for deleting the query history by index
        List<ConnectionDetails.QueryHistory> history = getConnection(connectionName).getHistory();
        if (index >= 0 && index < history.size()) {
            history.remove(index);
        }
        saveConnections();
    }

    /**
     * Executes a query on a connection.
     * 
     * @param connectionName the name of the connection
     * @param query          the query to execute
     * @return a list of RawJson documents
     * @throws ImportNetworkException if an error occurs during query execution
     */
    public List<RawJson> execute(String connectionName, String query) throws ImportNetworkException {
        return execute(connectionName, query, true);
    }

    /**
     * Executes a query on a connection.
     * 
     * @param connectionName the name of the connection
     * @param query          the query to execute
     * @param includeInHistory whether to include the query in the history
     * @return a list of RawJson documents
     * @throws ImportNetworkException if an error occurs during query execution
     */
    public List<RawJson> execute(String connectionName, String query, boolean includeInHistory)
            throws ImportNetworkException {
        this.validate(connectionName);
        ArangoDatabase database = getArangoDatabase(getConnection(connectionName));
        validate(database);

        List<RawJson> docs = database.query(query, RawJson.class).asListRemaining();
        if (includeInHistory) {
            this.addQueryToHistory(connectionName, query);
        }
        return docs;

    }

    /**
     * Gets the ArangoDatabase object for a connection with the specified name.
     * 
     * @param connectionName the name of the connection
     * @return the ArangoDatabase object
     */
    public ArangoDatabase getArangoDatabase(String connectionName) {
        return getArangoDatabase(getConnection(connectionName));
    }

    /**
     * Saves connections to a file.
     */
    private void saveConnections() {
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(connections, writer);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Loads connections from a file.
     */
    private void loadConnections() {
        try (Reader reader = new FileReader(filePath)) {
            connections = gson.fromJson(reader, new TypeToken<Map<String, ConnectionDetails>>() {
            }.getType());
            if (connections == null) {
                connections = new HashMap<>();
            }
        } catch (IOException e) {
            connections = new HashMap<>();
        }
    }

    /**
     * Validates a connection with the specified ArangoDatabase object.
     * 
     * @param db the ArangoDatabase object
     * @return true if the connection is valid, false otherwise
     */
    private boolean validate(ArangoDatabase db) {
        db.getVersion();
        return true;
    }

}