package com.github.hugowschneider.cyarangodb.internal.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.test.DependsOnConnectionManager;

@TestInstance(Lifecycle.PER_CLASS)
public class ArangoNetworkAdapterTest extends DependsOnConnectionManager {

    private NetworkViewTestSupport testSupport;
    private CyNetworkFactory networkFactory;
    private ArangoNetworkAdapter adapter;

    private static final String IMPORT_PATH_QUERY = "FOR n, e, p IN 1..1 ANY 'imdb_vertices/1000' GRAPH imdb\nRETURN p";
    private static final String IMPORT_EDGE_QUERY = "FOR e IN imdb_edges\nFILTER e._to == 'imdb_vertices/1000' or e._from == 'imdb_vertices/1000'\nRETURN e";

    private static final String EXPAND_NODE_ID = "imdb_vertices/21713";
    private static final String EXPAND_PATH_QUERY = "FOR n, e, p IN 1..1 ANY '" + EXPAND_NODE_ID
            + "' GRAPH imdb\nRETURN p";
    private static final String EXPAND_EDGE_QUERY = "FOR e IN imdb_edges\nFILTER e._to == '" + EXPAND_NODE_ID
            + "' or e._from == '" + EXPAND_NODE_ID +
            "'\nRETURN e";

    @BeforeAll
    public void setUpAll() {
        super.setupConnection();

        testSupport = new NetworkViewTestSupport();
        networkFactory = testSupport.getNetworkFactory();
    }

    @BeforeEach
    public void setUp() {
        adapter = new ArangoNetworkAdapter(connectionManager.getArangoDatabase(connectionId), networkFactory);
    }

    @AfterAll
    public void tearDownAll() {
        super.tearConnection();
    }

    private static final List<String> EXISTING_NODE_IDS = List.of(
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

    private static final Set<Map.Entry<String, String>> EXISTING_EDGES = Set.of(
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

    // Existing methods...

    private void assertNetwork(CyNetwork network, String query) {
        CyRow row = network.getDefaultNetworkTable().getRow(network.getSUID());
        String pluginMetadata = row.get("cyArangoDBMetadata", String.class);
        assertNotNull(pluginMetadata);
        ArangoNetworkMetadata metadata = adapter.deserialArangoNetworkMetadata(pluginMetadata);
        assertEquals(query, metadata.getQuery());
        assertEquals(connectionId, metadata.getConnectionId());

        // Check if the network contains the expected nodes
        assertEquals(EXISTING_NODE_IDS.size(), network.getNodeCount());

        for (CyNode node : network.getNodeList()) {
            CyRow nodeRow = network.getDefaultNodeTable().getRow(node.getSUID());
            String nodeId = nodeRow.get(Constants.NodeColumns.ID, String.class);
            assertTrue(EXISTING_NODE_IDS.contains(nodeId), "Unexpected node ID: " + nodeId);
        }

        assertEquals(EXISTING_EDGES.size(), network.getEdgeCount());

        for (CyEdge edge : network.getEdgeList()) {
            CyRow edgeRow = network.getDefaultEdgeTable().getRow(edge.getSUID());
            String sourceId = edgeRow.get(Constants.EdgeColumns.FROM, String.class);
            String targetId = edgeRow.get(Constants.EdgeColumns.TO, String.class);
            assertTrue(EXISTING_EDGES.contains(Map.entry(sourceId, targetId)),
                    "Unexpected edge: " + sourceId + " -> " + targetId);
        }
    }

    private void assertNodeExpansion(CyNetwork network, List<CyNode> newNodes, String expandNodeId) {
        List<String> expectedNewNodeIds = List.of(
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
                "imdb_vertices/1000",
                "imdb_vertices/789",
                "imdb_vertices/677",
                "imdb_vertices/403",
                "imdb_vertices/action");

        Set<Map.Entry<String, String>> expectedNewEdges = Set.of(
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
                Map.entry("imdb_vertices/1000", "imdb_vertices/21713"),
                Map.entry("imdb_vertices/789", "imdb_vertices/21713"),
                Map.entry("imdb_vertices/677", "imdb_vertices/21713"),
                Map.entry("imdb_vertices/403", "imdb_vertices/21713"),
                Map.entry("imdb_vertices/action", "imdb_vertices/21713"));

        // Combine existing and new node IDs
        for (CyNode node : newNodes) {
            CyRow nodeRow = network.getDefaultNodeTable().getRow(node.getSUID());
            String nodeId = nodeRow.get(Constants.NodeColumns.ID, String.class);

            assertTrue(expectedNewNodeIds.contains(nodeId), "Unexpected node ID: " + nodeId);
        }

        Set<String> allExpectedNodeIds = new HashSet<>(EXISTING_NODE_IDS);
        allExpectedNodeIds.addAll(expectedNewNodeIds);

        assertEquals(allExpectedNodeIds.size(), network.getNodeCount());

        for (CyNode node : network.getNodeList()) {
            CyRow nodeRow = network.getDefaultNodeTable().getRow(node.getSUID());
            String nodeId = nodeRow.get(Constants.NodeColumns.ID, String.class);
            assertTrue(allExpectedNodeIds.contains(nodeId), "Unexpected node ID: " + nodeId);
        }

        // Combine existing and new edges
        Set<Map.Entry<String, String>> allExpectedEdges = new HashSet<>(EXISTING_EDGES);
        allExpectedEdges.addAll(expectedNewEdges);

        long newEdgeCount = network.getEdgeList().stream()
                .filter(edge -> {
                    CyRow edgeRow = network.getDefaultEdgeTable().getRow(edge.getSUID());
                    String sourceId = edgeRow.get(Constants.EdgeColumns.FROM, String.class);
                    String targetId = edgeRow.get(Constants.EdgeColumns.TO, String.class);
                    return allExpectedEdges.contains(Map.entry(sourceId, targetId));
                })
                .count();

        assertEquals(allExpectedEdges.size(), newEdgeCount,
                "The number of new edges does not match the expected count.");
    }

    @Test
    @DisplayName("ArangoNetworkAdapter::importPaths Test if the network is correctly adapted from a path query")
    public void testImportNetworkPath() throws ImportNetworkException {
        List<RawJson> result = connectionManager.execute(connectionId, IMPORT_PATH_QUERY);
        CyNetwork network = adapter.importPaths(result, new ArangoNetworkMetadata(IMPORT_PATH_QUERY, connectionId));
        assertNetwork(network, IMPORT_PATH_QUERY);

    }

    @Test
    @DisplayName("ArangoNetworkAdapter::importEdges Test if the network is correctly adapted from an edge query")
    public void testImportNetworkEdge() throws ImportNetworkException {
        List<RawJson> result = connectionManager.execute(connectionId, IMPORT_EDGE_QUERY);
        CyNetwork network = adapter.importEdges(result, new ArangoNetworkMetadata(IMPORT_EDGE_QUERY, connectionId));
        assertNetwork(network, IMPORT_EDGE_QUERY);

    }

    @Test
    public void testExpandNetworkPath() throws ImportNetworkException {
        List<RawJson> result = connectionManager.execute(connectionId, IMPORT_PATH_QUERY);
        CyNetwork network = adapter.importPaths(result, new ArangoNetworkMetadata(IMPORT_PATH_QUERY, connectionId));

        result = connectionManager.execute(connectionId, EXPAND_PATH_QUERY);
        List<CyNode> newNodes = adapter.expandNodeWithPath(result,
                new ArangoNetworkMetadata.NodeExpansionMetadata(EXPAND_NODE_ID, EXPAND_PATH_QUERY, connectionId));

        assertNodeExpansion(network, newNodes, EXPAND_NODE_ID);
    }

    @Test
    public void testExpandNetworkEdge() throws ImportNetworkException {
        List<RawJson> result = connectionManager.execute(connectionId, IMPORT_EDGE_QUERY);
        CyNetwork network = adapter.importEdges(result, new ArangoNetworkMetadata(IMPORT_EDGE_QUERY, connectionId));

        result = connectionManager.execute(connectionId, EXPAND_EDGE_QUERY);
        List<CyNode> newNodes = adapter.expandNodeWithEdges(result,
                new ArangoNetworkMetadata.NodeExpansionMetadata(EXPAND_NODE_ID, EXPAND_EDGE_QUERY, connectionId));

        assertNodeExpansion(network, newNodes, EXPAND_NODE_ID);
    }

}
