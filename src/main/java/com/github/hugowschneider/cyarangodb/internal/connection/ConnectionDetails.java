package com.github.hugowschneider.cyarangodb.internal.connection;

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;

public class ConnectionDetails {
    private String host;
    private int port;
    private String user;
    private String password;
    private String database;
    private List<QueryHistory> history;

    public ConnectionDetails(String host, int port, String user, String password, String database) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
        this.history = new ArrayList<>();
    }

    // Getters and setters
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public List<QueryHistory> getHistory() {
        if (history == null) {
            history = new ArrayList<>();
        }
        return history;
    }

    public void addQueryToHistory(String query) {
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(new QueryHistory(query, LocalDateTime.now()));
    }

    public static class QueryHistory {
        private String query;
        private LocalDateTime executedAt;

        public QueryHistory(String query, LocalDateTime executedAt) {
            this.query = query;
            this.executedAt = executedAt;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public LocalDateTime getExecutedAt() {
            return executedAt;
        }

        public void setExecutedAt(LocalDateTime executedAt) {
            this.executedAt = executedAt;
        }

    }
}