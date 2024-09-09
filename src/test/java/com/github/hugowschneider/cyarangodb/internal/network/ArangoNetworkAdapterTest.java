package com.github.hugowschneider.cyarangodb.internal.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.github.hugowschneider.cyarangodb.internal.network.ArangoNetworkMetadata.NetworkExpansionMetadata;
import com.github.hugowschneider.cyarangodb.internal.network.ArangoNetworkMetadata.NodeExpansionMetadata;
import com.github.hugowschneider.cyarangodb.internal.test.DependsOnConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.test.Helper;

@TestInstance(Lifecycle.PER_CLASS)
public class ArangoNetworkAdapterTest extends DependsOnConnectionManager {

    private NetworkViewTestSupport testSupport;
    private CyNetworkFactory networkFactory;
    private ArangoNetworkAdapter adapter;

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

    private void assertNetwork(CyNetwork network, String query) {
        CyRow row = network.getDefaultNetworkTable().getRow(network.getSUID());
        String pluginMetadata = row.get("cyArangoDBMetadata", String.class);
        assertNotNull(pluginMetadata);
        ArangoNetworkMetadata metadata = adapter.deserialArangoNetworkMetadata(pluginMetadata);
        assertEquals(query, metadata.getQuery());
        assertEquals(connectionId, metadata.getConnectionId());

        // Check if the network contains the expected nodes
        assertEquals(Helper.EXISTING_NODE_IDS.size(), network.getNodeCount());

        for (CyNode node : network.getNodeList()) {
            CyRow nodeRow = network.getDefaultNodeTable().getRow(node.getSUID());
            String nodeId = nodeRow.get(Constants.NodeColumns.ID, String.class);
            assertTrue(Helper.EXISTING_NODE_IDS.contains(nodeId), "Unexpected node ID: " + nodeId);
        }

        assertEquals(Helper.EXISTING_EDGES.size(), network.getEdgeCount());

        for (CyEdge edge : network.getEdgeList()) {
            CyRow edgeRow = network.getDefaultEdgeTable().getRow(edge.getSUID());
            String sourceId = edgeRow.get(Constants.EdgeColumns.FROM, String.class);
            String targetId = edgeRow.get(Constants.EdgeColumns.TO, String.class);
            assertTrue(Helper.EXISTING_EDGES.contains(Map.entry(sourceId, targetId)),
                    "Unexpected edge: " + sourceId + " -> " + targetId);
        }
    }

    private void assertExpansion(CyNetwork network, List<CyNode> newNodes, String query, String connectionId,
            String expandNodeId) {

        // Combine existing and new node IDs

        for (CyNode node : newNodes) {
            CyRow nodeRow = network.getDefaultNodeTable().getRow(node.getSUID());
            String nodeId = nodeRow.get(Constants.NodeColumns.ID, String.class);

            assertTrue(Helper.EXPECTED_NEW_NODE_IDS.contains(nodeId), "Unexpected node ID: " + nodeId);
        }
        assertEquals(Helper.EXPECTED_NEW_NODE_IDS.stream().sorted().toList(), newNodes.stream()
                .map(node -> network.getDefaultNodeTable().getRow(node.getSUID()).get(Constants.NodeColumns.ID,
                        String.class))
                .sorted()
                .toList());
        Set<String> allExpectedNodeIds = new HashSet<>(Helper.EXISTING_NODE_IDS);
        allExpectedNodeIds.addAll(Helper.EXPECTED_NEW_NODE_IDS);

        assertEquals(allExpectedNodeIds.size(), network.getNodeCount());

        for (CyNode node : network.getNodeList()) {
            CyRow nodeRow = network.getDefaultNodeTable().getRow(node.getSUID());
            String nodeId = nodeRow.get(Constants.NodeColumns.ID, String.class);
            assertTrue(allExpectedNodeIds.contains(nodeId), "Unexpected node ID: " + nodeId);
        }

        // Combine existing and new edges
        Set<Map.Entry<String, String>> allExpectedEdges = new HashSet<>(Helper.EXISTING_EDGES);
        allExpectedEdges.addAll(Helper.EXPECTED_NEW_EDGES);

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

        CyRow row = network.getDefaultNetworkTable().getRow(network.getSUID());
        String pluginMetadata = row.get("cyArangoDBMetadata", String.class);
        assertNotNull(pluginMetadata);
        ArangoNetworkMetadata metadata = adapter.deserialArangoNetworkMetadata(pluginMetadata);
        if (expandNodeId != null) {
            NodeExpansionMetadata nodeMetadata = metadata.getNodeExpansions().getFirst();
            assertEquals(expandNodeId, nodeMetadata.getNodeId());
            assertEquals(query, nodeMetadata.getQuery());
            assertEquals(connectionId, nodeMetadata.getConnectionId());

        } else {
            NetworkExpansionMetadata nodeMetadata = metadata.getNetworkExpansions().getFirst();
            assertEquals(query, nodeMetadata.getQuery());
            assertEquals(connectionId, nodeMetadata.getConnectionId());

        }
    }

    @Test
    @DisplayName("ArangoNetworkAdapter::importPaths Test if the network is correctly adapted from a path query")
    public void testImportNetworkPath() throws ImportNetworkException {
        List<RawJson> result = connectionManager.execute(connectionId, Helper.IMPORT_PATH_QUERY);
        CyNetwork network = adapter.importPaths(result,
                new ArangoNetworkMetadata(Helper.IMPORT_PATH_QUERY, connectionId));
        assertNetwork(network, Helper.IMPORT_PATH_QUERY);

    }

    @Test
    @DisplayName("ArangoNetworkAdapter::importEdges Test if the network is correctly adapted from an edge query")
    public void testImportNetworkEdge() throws ImportNetworkException {
        List<RawJson> result = connectionManager.execute(connectionId, Helper.IMPORT_EDGE_QUERY);
        CyNetwork network = adapter.importEdges(result,
                new ArangoNetworkMetadata(Helper.IMPORT_EDGE_QUERY, connectionId));
        assertNetwork(network, Helper.IMPORT_EDGE_QUERY);

    }

    @Test
    public void testExpandNodekPath() throws ImportNetworkException {
        List<RawJson> result = connectionManager.execute(connectionId, Helper.IMPORT_PATH_QUERY);
        CyNetwork network = adapter.importPaths(result,
                new ArangoNetworkMetadata(Helper.IMPORT_PATH_QUERY, connectionId));

        result = connectionManager.execute(connectionId, Helper.EXPAND_PATH_QUERY);
        List<CyNode> newNodes = adapter.expandNodeWithPath(result,
                new ArangoNetworkMetadata.NodeExpansionMetadata(Helper.EXPAND_NODE_ID, Helper.EXPAND_PATH_QUERY,
                        connectionId));

        assertExpansion(network, newNodes, Helper.EXPAND_PATH_QUERY, connectionId, Helper.EXPAND_NODE_ID);
    }

    @Test
    public void testExpandNodeEdge() throws ImportNetworkException {
        List<RawJson> result = connectionManager.execute(connectionId, Helper.IMPORT_EDGE_QUERY);
        CyNetwork network = adapter.importEdges(result,
                new ArangoNetworkMetadata(Helper.IMPORT_EDGE_QUERY, connectionId));

        result = connectionManager.execute(connectionId, Helper.EXPAND_EDGE_QUERY);
        List<CyNode> newNodes = adapter.expandNodeWithEdges(result,
                new ArangoNetworkMetadata.NodeExpansionMetadata(Helper.EXPAND_NODE_ID, Helper.EXPAND_EDGE_QUERY,
                        connectionId));

        assertExpansion(network, newNodes, Helper.EXPAND_EDGE_QUERY, connectionId, Helper.EXPAND_NODE_ID);
    }

    @Test
    public void testExpandNetworkPath() throws ImportNetworkException {
        List<RawJson> result = connectionManager.execute(connectionId, Helper.IMPORT_PATH_QUERY);
        CyNetwork network = adapter.importPaths(result,
                new ArangoNetworkMetadata(Helper.IMPORT_PATH_QUERY, connectionId));

        result = connectionManager.execute(connectionId, Helper.EXPAND_PATH_QUERY);
        List<CyNode> newNodes = adapter.expandNetworkPaths(result,
                new ArangoNetworkMetadata.NetworkExpansionMetadata(Helper.EXPAND_PATH_QUERY, connectionId));

        assertExpansion(network, newNodes, Helper.EXPAND_PATH_QUERY, connectionId, null);
    }

    @Test
    public void testExpandNetworkEdge() throws ImportNetworkException {
        List<RawJson> result = connectionManager.execute(connectionId, Helper.IMPORT_EDGE_QUERY);
        CyNetwork network = adapter.importEdges(result,
                new ArangoNetworkMetadata(Helper.IMPORT_EDGE_QUERY, connectionId));

        result = connectionManager.execute(connectionId, Helper.EXPAND_EDGE_QUERY);
        List<CyNode> newNodes = adapter.expandNetworkEdges(result,
                new ArangoNetworkMetadata.NetworkExpansionMetadata(Helper.EXPAND_EDGE_QUERY, connectionId));

        assertExpansion(network, newNodes, Helper.EXPAND_EDGE_QUERY, connectionId, null);
    }

}
