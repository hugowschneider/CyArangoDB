package com.github.hugowschneider.cyarangodb.internal.connection;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

public class ConnectionManager {
    private Map<String, ConnectionDetails> connections;

    private static final String FILE_PATH = System.getProperty("user.home") + File.separator + "CytoscapeConfiguration"
            + File.separator + "arangodb-connection.json";
    private final Gson gson;

    public ConnectionManager() {
        connections = new HashMap<>();

        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        loadConnections();

    }

    // Method to add a connection
    public void addConnection(String name, ConnectionDetails details) {
        connections.put(name, details);
        saveConnections();
    }

    // Method to remove a connection
    public void removeConnection(String name) {
        connections.remove(name);
        saveConnections();
    }

    // Method to get a connection
    public ConnectionDetails getConnection(String name) {
        return connections.get(name);
    }

    // Method to get all connections
    public Map<String, ConnectionDetails> getAllConnections() {
        return new HashMap<>(connections);
    }

    // Method to save connections to a file
    private void saveConnections() {
        try (Writer writer = new FileWriter(FILE_PATH)) {
            gson.toJson(connections, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to load connections from a file
    private void loadConnections() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            connections = gson.fromJson(reader, new TypeToken<Map<String, ConnectionDetails>>() {
            }.getType());
            if (connections == null) {
                connections = new HashMap<>();
            }
        } catch (IOException e) {
            connections = new HashMap<>();
        }
    }

    // Method to add a query to history
    public void addQueryToHistory(String connectionName, String query) {
        this.getConnection(connectionName).addQueryToHistory(query);
        this.saveConnections();

    }

    // Method to get query history for a connection
    public List<ConnectionDetails.QueryHistory> getQueryHistory(String connectionName) {
        return this.getConnection(connectionName).getHistory();
    }

    public boolean validate(String name) {
        return this.validate(this.getConnection(name));
    }

    public ArangoDatabase getArangoDatabase(ConnectionDetails connectionDetails) {
        ArangoDB arangoDB = new ArangoDB.Builder().host(connectionDetails.getHost(), connectionDetails.getPort())
                .user(connectionDetails.getUser()).password(connectionDetails.getPassword())
                .protocol(Protocol.HTTP2_VPACK).build();

        return arangoDB.db(connectionDetails.getDatabase());
    }

    public boolean validate(ConnectionDetails connectionDetails) {

        ArangoDatabase db = getArangoDatabase(connectionDetails);
        return validate(db);

    }

    private boolean validate(ArangoDatabase db) {

        db.getVersion();

        return true;

    }

    public List<RawJson> runHistory(String connectionName, int index) throws ImportNetworkException {
        List<ConnectionDetails.QueryHistory> history = getConnection(connectionName).getHistory();
        return execute(connectionName, history.get(index).getQuery(), false);

    }

    public void deleteQueryHistory(String connectionName, int index) {
        // Implementation for deleting the query history by index
        List<ConnectionDetails.QueryHistory> history = getConnection(connectionName).getHistory();
        if (index >= 0 && index < history.size()) {
            history.remove(index);
        }
    }

    public List<RawJson> execute(String connectionName, String query) throws ImportNetworkException {
        return execute(connectionName, query, true);
    }

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

    static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public void write(JsonWriter jsonWriter, LocalDateTime localDateTime) throws IOException {
            jsonWriter.value(localDateTime.format(formatter));
        }

        @Override
        public LocalDateTime read(JsonReader jsonReader) throws IOException {
            return LocalDateTime.parse(jsonReader.nextString(), formatter);
        }
    }

    public ArangoDatabase getArangoDatabase(String connectionName) {
        return getArangoDatabase(getConnection(connectionName));
    }

}