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

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabase() {
        return database;
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

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ConnectionDetails)) {
            return false;
        }
        ConnectionDetails other = (ConnectionDetails) obj;
        return this.host.equals(other.host) && this.port == other.port && this.user.equals(other.user)
                && this.password.equals(other.password) && this.database.equals(other.database)
                && this.history.equals(other.history);
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

        public LocalDateTime getExecutedAt() {
            return executedAt;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof QueryHistory)) {
                return false;
            }
            QueryHistory other = (QueryHistory) obj;
            return this.query.equals(other.query) && this.executedAt.equals(other.executedAt);
        }

    }
}