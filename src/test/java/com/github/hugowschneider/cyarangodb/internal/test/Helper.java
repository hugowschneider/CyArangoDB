package com.github.hugowschneider.cyarangodb.internal.test;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails;

public class Helper {

    public static ConnectionDetails createConnectionDetails() {
        String host = System.getenv("ARANGODB_HOST");
        int port = Integer.parseInt(System.getenv("ARANGODB_PORT"));
        String username = System.getenv("ARANGODB_USERNAME");
        String password = System.getenv("ARANGODB_PASSWORD");
        String database = System.getenv("ARANGODB_DATABASE");

        return new ConnectionDetails(host, port, username, password, database);
    }

}
