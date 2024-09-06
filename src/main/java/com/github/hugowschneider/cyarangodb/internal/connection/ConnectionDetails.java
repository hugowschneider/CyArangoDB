package com.github.hugowschneider.cyarangodb.internal.connection;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Represents the details required to establish a connection to a database.
 */
public class ConnectionDetails {
    private String host;
    private int port;
    private String user;
    private String password;
    private String database;
    private List<QueryHistory> history;
    private String name;

    /**
     * Constructs a new ConnectionDetails instance.
     *
     * @param host     the database host
     * @param port     the database port
     * @param user     the database user
     * @param password the database password
     * @param database the database name
     */
    public ConnectionDetails(String name, String host, int port, String user, String password, String database) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
        this.history = new ArrayList<>();
    }

    /**
     * Gets the database host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the connection name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the database port.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the database user.
     *
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * Gets the database password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the database name.
     *
     * @return the database
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Gets the query history.
     *
     * @return the history
     */
    public List<QueryHistory> getHistory() {
        if (history == null) {
            history = new ArrayList<>();
        }
        return history;
    }

    /**
     * Adds a query to the history.
     *
     * @param query the query to add
     * @return the time the query was executed
     */
    public LocalDateTime addQueryToHistory(String query) {
        if (history == null) {
            history = new ArrayList<>();
        }
        LocalDateTime now = LocalDateTime.now();
        history.add(new QueryHistory(query, now));
        return now;
    }

    /**
     * Checks if this ConnectionDetails instance is equal to another object.
     *
     * @param obj the object to compare
     * @return true if the objects are equal, false otherwise
     */
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

    /**
     * Represents a history of executed queries.
     */
    public static class QueryHistory {
        private String query;
        private LocalDateTime executedAt;

        /**
         * Constructs a new QueryHistory instance.
         *
         * @param query      the executed query
         * @param executedAt the time the query was executed
         */
        public QueryHistory(String query, LocalDateTime executedAt) {
            this.query = query;
            this.executedAt = executedAt;
        }

        /**
         * Gets the executed query.
         *
         * @return the query
         */
        public String getQuery() {
            return query;
        }

        /**
         * Gets the time the query was executed.
         *
         * @return the executedAt
         */
        public LocalDateTime getExecutedAt() {
            return executedAt;
        }

        /**
         * Checks if this QueryHistory instance is equal to another object.
         *
         * @param obj the object to compare
         * @return true if the objects are equal, false otherwise
         */
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