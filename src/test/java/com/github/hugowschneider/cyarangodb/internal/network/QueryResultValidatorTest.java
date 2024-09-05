package com.github.hugowschneider.cyarangodb.internal.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.test.Helper;

@TestInstance(Lifecycle.PER_CLASS)
public class QueryResultValidatorTest {

    private ConnectionManager connectionManager;
    private static final String CONNECTION_NAME = "test-connection";

    @BeforeAll
    public void setUpAll() {
        ConnectionDetails connectionDetails = Helper.createConnectionDetails();
        connectionManager = new ConnectionManager("./target/test-resources");
        connectionManager.addConnection(CONNECTION_NAME, connectionDetails);

    }

    @AfterAll
    public void tearDownAll() {
        File file = new File(connectionManager.getFilePath());
        file.delete();
    }

    @Test
    @DisplayName("QueryResultValidator::isPathList should return true")
    public void testIsPathList() throws ImportNetworkException {

        List<RawJson> docs = connectionManager.execute(CONNECTION_NAME,
                "FOR v, e, p IN 1..1 ANY 'imdb_vertices/1000' GRAPH imdb\nRETURN p");
        QueryResultValidator queryResultValidator = new QueryResultValidator(docs);
        assertTrue(queryResultValidator.isPathList());
        assertFalse(queryResultValidator.isEdgeList());
    }

    @Test
    @DisplayName("QueryResultValidator::isEdgeList shpuld reutrn true")
    public void testIsEdgeList() throws ImportNetworkException {

        List<RawJson> docs = connectionManager.execute(CONNECTION_NAME,
                "FOR e IN imdb_edges\nLIMIT 10\nRETURN e");
        QueryResultValidator queryResultValidator = new QueryResultValidator(docs);
        assertTrue(queryResultValidator.isEdgeList());
        assertFalse(queryResultValidator.isPathList());
    }

    @Test
    @DisplayName("QueryResultValidator::isNodeEdgePresent shpuld reutrn true in a path list")
    public void testIsNodePresentInPathList() throws ImportNetworkException {

        List<RawJson> docs = connectionManager.execute(CONNECTION_NAME,
                "FOR v, e, p IN 1..1 ANY 'imdb_vertices/1000' GRAPH imdb\nRETURN p");
        QueryResultValidator queryResultValidator = new QueryResultValidator(docs);
        assertTrue(queryResultValidator.isPathList());
        assertTrue(queryResultValidator.isNodeEdgePresent("imdb_vertices/1000"));
        assertFalse(queryResultValidator.isNodeEdgePresent("imdb_vertices/x1000"));
    }

    @Test
    @DisplayName("QueryResultValidator::isEdgeList shpuld reutrn true")
    public void testIsNodePresentInEdgeList() throws ImportNetworkException {

            List<RawJson> docs = connectionManager.execute(CONNECTION_NAME,
                "FOR e IN imdb_edges\nFILTER e._from == 'imdb_vertices/1000'\nLIMIT 10\nRETURN e");
        QueryResultValidator queryResultValidator = new QueryResultValidator(docs);
        assertTrue(queryResultValidator.isEdgeList());
        assertTrue(queryResultValidator.isNodeEdgePresent("imdb_vertices/1000"));
        assertFalse(queryResultValidator.isNodeEdgePresent("imdb_vertices/x1000"));
    }

}
