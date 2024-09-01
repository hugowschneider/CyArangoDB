package com.github.hugowschneider.cyarangodb.internal.connection;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.Protocol;
import com.arangodb.entity.BaseDocument;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ConnectionManager {
    private static ConnectionManager instance;
    private Map<String, ConnectionDetails> connections;
    private static final String FILE_PATH = System.getProperty("user.home") + File.separator + "CytoscapeConfiguration"
            + File.separator + "arangodb-connection.json";
    private final Gson gson;

    // Private constructor to prevent instantiation
    private ConnectionManager() {
        connections = new HashMap<>();
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        loadConnections();

    }

    // Public method to provide access to the instance
    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
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
        db.getVersion();
        return true;

    }

    public void execute(String connectionName, String query) {
        System.out.println("Executing query: " + query);

        ArangoDatabase database = getArangoDatabase(getConnection(connectionName));
        database.getVersion();
        ArangoCursor<BaseDocument> docs = database.query(query, BaseDocument.class);

        docs.forEach(doc -> {
            System.out.println(doc.toString());
        });

        addQueryToHistory(connectionName, query);
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

}