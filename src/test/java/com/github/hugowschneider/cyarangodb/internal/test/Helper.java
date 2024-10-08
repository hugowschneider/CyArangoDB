package com.github.hugowschneider.cyarangodb.internal.test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails.ConnectionProtocol;

public class Helper {

        public static ConnectionDetails createConnectionDetails() {
                String name = "TestConnection";
                String host = System.getenv("ARANGODB_HOST");
                int port = Integer.parseInt(System.getenv("ARANGODB_PORT"));
                String username = System.getenv("ARANGODB_USERNAME");
                String password = System.getenv("ARANGODB_PASSWORD");
                String database = System.getenv("ARANGODB_DATABASE");

                return new ConnectionDetails(name, host, port, username, password, database, ConnectionProtocol.HTTP2);
        }

        public static final String IMPORT_PATH_QUERY = "FOR n, e, p IN 1..1 ANY 'imdb_vertices/1000' GRAPH imdb\nRETURN p";
        public static final String IMPORT_EDGE_QUERY = "FOR e IN imdb_edges\nFILTER e._to == 'imdb_vertices/1000' or e._from == 'imdb_vertices/1000'\nRETURN e";
        public static final String EXPAND_NODE_ID = "imdb_vertices/21713";
        public static final String EXPAND_PATH_QUERY = "FOR n, e, p IN 1..1 ANY '" + EXPAND_NODE_ID
                        + "' GRAPH imdb\nRETURN p";
        public static final String EXPAND_EDGE_QUERY = "FOR e IN imdb_edges\nFILTER e._to == '" + EXPAND_NODE_ID
                        + "' or e._from == '" + EXPAND_NODE_ID +
                        "'\nRETURN e";
        public static final List<String> EXISTING_NODE_IDS = List.of(
                        "imdb_vertices/1000",
                        "imdb_vertices/2445",
                        "imdb_vertices/4270",
                        "imdb_vertices/10805",
                        "imdb_vertices/12266",
                        "imdb_vertices/13655",
                        "imdb_vertices/21713",
                        "imdb_vertices/22969",
                        "imdb_vertices/32820",
                        "imdb_vertices/34775",
                        "imdb_vertices/36776",
                        "imdb_vertices/38639",
                        "imdb_vertices/39965",
                        "imdb_vertices/52708",
                        "imdb_vertices/54917",
                        "imdb_vertices/59004",
                        "imdb_vertices/59747",
                        "imdb_vertices/67802",
                        "imdb_vertices/998");
        public static final Set<Map.Entry<String, String>> EXISTING_EDGES = Set.of(
                        Map.entry("imdb_vertices/1000", "imdb_vertices/2445"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/4270"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/10805"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/12266"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/13655"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/22969"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/32820"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/34775"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/36776"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/38639"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/39965"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/52708"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/54917"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/59004"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/59747"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/67802"),
                        Map.entry("imdb_vertices/1000", "imdb_vertices/998"));
        public static final Set<Map.Entry<String, String>> EXPECTED_NEW_EDGES = Set.of(
                        Map.entry("imdb_vertices/21714", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/21715", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/21716", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/15141", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/13893", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/7359", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/6580", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/5407", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/5084", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/4984", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/1931", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/789", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/677", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/403", "imdb_vertices/21713"),
                        Map.entry("imdb_vertices/action", "imdb_vertices/21713"));
        public static final List<String> EXPECTED_NEW_NODE_IDS = List.of(
                        "imdb_vertices/21714",
                        "imdb_vertices/21715",
                        "imdb_vertices/21716",
                        "imdb_vertices/15141",
                        "imdb_vertices/13893",
                        "imdb_vertices/7359",
                        "imdb_vertices/6580",
                        "imdb_vertices/5407",
                        "imdb_vertices/5084",
                        "imdb_vertices/4984",
                        "imdb_vertices/1931",
                        "imdb_vertices/789",
                        "imdb_vertices/677",
                        "imdb_vertices/403",
                        "imdb_vertices/action");

}
