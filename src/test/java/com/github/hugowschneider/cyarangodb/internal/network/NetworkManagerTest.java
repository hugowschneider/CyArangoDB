package com.github.hugowschneider.cyarangodb.internal.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.ArgumentMatcher;

import com.arangodb.ArangoDatabase;
import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.test.DependsOnConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.test.Helper;

@TestInstance(Lifecycle.PER_CLASS)
public class NetworkManagerTest extends DependsOnConnectionManager {

    private CyNetworkFactory cyNetworkFactory;
    private CyNetworkManager cyNetworkManager;
    private CyNetworkViewFactory cyNetworkViewFactory;
    private CyNetworkViewManager cyNetworkViewManager;
    private CyApplicationManager cyApplicationManager;
    private CyLayoutAlgorithmManager cyLayoutAlgorithmManager;
    private TaskManager<?, ?> taskManager;
    private ArangoNetworkStyle arangoNetworkStyle;
    private NetworkManager networkManager;
    private NetworkViewTestSupport networkViewTestSupport;
    private CyLayoutAlgorithm cyLayoutAlgorithm;
    private CyNetworkView networkView;

    @BeforeAll
    public void setUpAll() {
        super.setupConnection();

    }

    @BeforeEach
    public void setUp() {

        networkViewTestSupport = new NetworkViewTestSupport();
        cyNetworkFactory = networkViewTestSupport.getNetworkFactory();
        cyNetworkManager = networkViewTestSupport.getNetworkManager();
        cyNetworkViewFactory = networkViewTestSupport.getNetworkViewFactory();

        cyNetworkViewManager = mock(CyNetworkViewManager.class);
        cyApplicationManager = mock(CyApplicationManager.class);
        cyLayoutAlgorithmManager = mock(CyLayoutAlgorithmManager.class);
        cyLayoutAlgorithm = mock(CyLayoutAlgorithm.class);
        when(cyLayoutAlgorithm.getDefaultLayoutContext()).thenReturn(null);
        when(cyLayoutAlgorithm.createTaskIterator(
                any(CyNetworkView.class),
                or(any(Object.class), isNull(Object.class)),
                anySet(), or(isNull(String.class), anyString())))
                .thenReturn(new TaskIterator(new Task() {

                    public void run(TaskMonitor taskMonitor) throws Exception {
                        // Do nothing
                    }

                    @Override
                    public void cancel() {

                    }
                }));
        when(cyLayoutAlgorithmManager.getDefaultLayout()).thenReturn(cyLayoutAlgorithm);
        taskManager = mock(TaskManager.class);
        arangoNetworkStyle = mock(ArangoNetworkStyle.class);

        // Create instance of NetworkManager with mocked dependencies
        networkManager = new NetworkManager(
                cyNetworkFactory,
                cyNetworkManager,
                cyNetworkViewFactory,
                cyNetworkViewManager,
                cyApplicationManager,
                cyLayoutAlgorithmManager,
                taskManager,
                arangoNetworkStyle);
    }

    @Test
    @DisplayName("NetworkManager::importNetwork should throw ImportNetworkException when query result is not a edge or a path list")
    public void testInvalidQueryResult() {
        String query = "FOR v IN imdb_vertices RETURN v";
        List<RawJson> docs = connectionManager.execute(this.connectionId, query);
        ArangoDatabase database = connectionManager.getArangoDatabase(this.connectionId);

        assertThrows(ImportNetworkException.class, () -> networkManager.importNetwork(docs, database, "imdb",
                new ArangoNetworkMetadata(query, connectionId)));
    }

    @Test
    @DisplayName("NetworkManager::importNetwork should import and apply layout when importing an edge collection")
    public void testImportEdgeList() throws ImportNetworkException {
        List<RawJson> docs = connectionManager.execute(this.connectionId, Helper.IMPORT_EDGE_QUERY);
        ArangoDatabase database = connectionManager.getArangoDatabase(this.connectionId);

        testImportNetwork(docs, database, Helper.IMPORT_EDGE_QUERY);
    }

    @Test
    @DisplayName("NetworkManager::importNetwork should import and apply layout when importing a path collection")
    public void testImportPathList() throws ImportNetworkException {
        List<RawJson> docs = connectionManager.execute(this.connectionId, Helper.IMPORT_PATH_QUERY);
        ArangoDatabase database = connectionManager.getArangoDatabase(this.connectionId);

        testImportNetwork(docs, database, Helper.IMPORT_PATH_QUERY);
    }

    private void testImportNetwork(List<RawJson> docs, ArangoDatabase database, String query)
            throws ImportNetworkException {
        NetworkImportResult importResult = networkManager.importNetwork(docs, database, "imdb",
                new ArangoNetworkMetadata(query, connectionId));

        Optional<CyNetwork> network = cyNetworkManager.getNetworkSet().stream().filter(n -> {
            return n.getDefaultNetworkTable().getRow(n.getSUID()).get(CyNetwork.NAME,
                    String.class).equals("imdb");
        }).findFirst();
        assertTrue(network.isPresent());

        assertEquals(importResult.getNodeCount(), Helper.EXISTING_NODE_IDS.size());
        assertEquals(importResult.getEdgeCount(), Helper.EXISTING_EDGES.size());
        verify(cyNetworkViewManager, times(1)).addNetworkView(any(CyNetworkView.class));

        verify(cyLayoutAlgorithm, times(1)).createTaskIterator(
                any(CyNetworkView.class),
                or(any(Object.class), isNull(Object.class)),
                argThat(new ArgumentMatcher<Set<View<CyNode>>>() {

                    @Override
                    public boolean matches(Set<View<CyNode>> nodes) {
                        List<String> ids = nodes.stream().map(node -> {
                            return network.get().getDefaultNodeTable().getRow(node.getModel().getSUID())
                                    .get(Constants.NodeColumns.ID, String.class);
                        }).collect(Collectors.toList());

                        return ids.size() == Helper.EXISTING_NODE_IDS.size()
                                && ids.containsAll(Helper.EXISTING_NODE_IDS);
                    }

                }), or(isNull(String.class), anyString()));

        assertTrue(networkManager.getAllNetworkNames().contains("imdb"));
    }

    @Test
    @DisplayName("NetworkManager::expandNetwork should expand network and apply layout when expanding with path collection")
    public void testExtendPathList() throws ImportNetworkException {
        testExpandNetwork(Helper.IMPORT_PATH_QUERY, Helper.EXPAND_PATH_QUERY);
    }

    @Test
    @DisplayName("NetworkManager::expandNetwork should expand network and apply layout when expanding with edge collection")
    public void testExtendEdgeList() throws ImportNetworkException {
        testExpandNetwork(Helper.IMPORT_PATH_QUERY, Helper.EXPAND_EDGE_QUERY);
    }

    @Test
    @DisplayName("NetworkManager::expandNetwork should throw ImportNetworkException when query result is not a edge or a path list")
    public void testExtendNetworkShouldThrow() throws ImportNetworkException {
        networkView = null;

        doAnswer(invocation -> {
            networkView = invocation.getArgument(0);
            return null;
        }).when(cyNetworkViewManager).addNetworkView(any(CyNetworkView.class));

        List<RawJson> importDocs = connectionManager.execute(this.connectionId, Helper.IMPORT_PATH_QUERY);
        ArangoDatabase database = connectionManager.getArangoDatabase(this.connectionId);

        networkManager.importNetwork(importDocs, database, "imdb",
                new ArangoNetworkMetadata(Helper.IMPORT_PATH_QUERY, connectionId));

        reset(cyNetworkViewManager);
        reset(cyLayoutAlgorithm);

        String query = "FOR v IN imdb_vertices RETURN v";
        // Expand network
        List<RawJson> expandDocs = connectionManager.execute(this.connectionId, query);
        assertThrows(ImportNetworkException.class, () -> {
            networkManager.expandNetwork(expandDocs, networkView, database,
                    new ArangoNetworkMetadata.NetworkExpansionMetadata(query, connectionId));
        });

    }

    private void testExpandNetwork(String importQuery, String expandQuery) throws ImportNetworkException {
        testExpandNetwork(importQuery, expandQuery, null);
    }

    private void testExpandNetwork(String importQuery, String expandQuery, String expandNodeId)
            throws ImportNetworkException {
        // Test setup
        networkView = null;
        View<CyNode> fromNodeView = null;

        doAnswer(invocation -> {
            networkView = invocation.getArgument(0);
            return null;
        }).when(cyNetworkViewManager).addNetworkView(any(CyNetworkView.class));

        List<RawJson> importDocs = connectionManager.execute(this.connectionId, importQuery);
        ArangoDatabase database = connectionManager.getArangoDatabase(this.connectionId);

        networkManager.importNetwork(importDocs, database, "imdb",
                new ArangoNetworkMetadata(importQuery, connectionId));

        reset(cyNetworkViewManager);
        reset(cyLayoutAlgorithm);

        // Find the node to expand from if expandNodeId is provided
        if (expandNodeId != null) {
            CyNetwork network = networkView.getModel();
            CyNode fromNode = network.getNodeList().stream()
                    .filter(node -> expandNodeId.equals(network.getDefaultNodeTable().getRow(node.getSUID())
                            .get(Constants.NodeColumns.ID, String.class)))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Node to expand from not found"));

            fromNodeView = networkView.getNodeView(fromNode);
        }

        // Expand network
        List<RawJson> expandDocs = connectionManager.execute(this.connectionId, expandQuery);
        List<CyNode> newNodes;
        if (fromNodeView != null) {
            newNodes = networkManager.expandNetwork(expandDocs, networkView, fromNodeView, database,
                    new ArangoNetworkMetadata.NodeExpansionMetadata(expandNodeId, expandQuery, connectionId));
        } else {
            newNodes = networkManager.expandNetwork(expandDocs, networkView, database,
                    new ArangoNetworkMetadata.NetworkExpansionMetadata(expandQuery, connectionId));
        }

        CyNetwork network = networkView.getModel();

        // Verify results
        assertEquals(Helper.EXPECTED_NEW_NODE_IDS.size(), newNodes.size());
        assertEquals(network.getEdgeCount(), Helper.EXISTING_EDGES.size() + Helper.EXPECTED_NEW_EDGES.size());
        verify(cyNetworkViewManager, times(0)).addNetworkView(any(CyNetworkView.class));

        verify(cyLayoutAlgorithm, times(1)).createTaskIterator(
                any(CyNetworkView.class),
                or(any(Object.class), isNull(Object.class)),
                anySet(), or(isNull(String.class), anyString()));

        assertEquals(1, networkManager.getAllNetworkNames().size());
        assertTrue(networkManager.getAllNetworkNames().contains("imdb"));

        // Sort and compare node IDs
        List<String> expectedNodeIds = Helper.EXPECTED_NEW_NODE_IDS.stream().sorted().collect(Collectors.toList());
        List<String> actualNodeIds = newNodes.stream()
                .map(node -> network.getDefaultNodeTable().getRow(node.getSUID()).get(Constants.NodeColumns.ID,
                        String.class))
                .sorted()
                .collect(Collectors.toList());

        assertEquals(expectedNodeIds, actualNodeIds);
    }

    @Test
    @DisplayName("NetworkManager::expandNetwork should expand network from a node view and apply layout when expanding with path collection")
    public void testExpandFromNodePathList() throws ImportNetworkException {
        testExpandNetworkFromNode(Helper.IMPORT_PATH_QUERY, Helper.EXPAND_PATH_QUERY);
    }

    @Test
    @DisplayName("NetworkManager::expandNetwork should expand network from a node view and apply layout when expanding with edge collection")
    public void testExpandFromNodeEdgeList() throws ImportNetworkException {
        testExpandNetworkFromNode(Helper.IMPORT_PATH_QUERY, Helper.EXPAND_EDGE_QUERY);
    }

    private void testExpandNetworkFromNode(String importQuery, String expandQuery) throws ImportNetworkException {
        // Test setup
        networkView = null;
        View<CyNode> fromNodeView = null;

        doAnswer(invocation -> {
            networkView = invocation.getArgument(0);
            return null;
        }).when(cyNetworkViewManager).addNetworkView(any(CyNetworkView.class));

        List<RawJson> importDocs = connectionManager.execute(this.connectionId, importQuery);
        ArangoDatabase database = connectionManager.getArangoDatabase(this.connectionId);

        networkManager.importNetwork(importDocs, database, "imdb",
                new ArangoNetworkMetadata(importQuery, connectionId));

        reset(cyNetworkViewManager);
        reset(cyLayoutAlgorithm);

        // Find the node to expand from
        CyNetwork network = networkView.getModel();
        CyNode fromNode = network.getNodeList().stream()
                .filter(node -> Helper.EXPAND_NODE_ID.equals(network.getDefaultNodeTable().getRow(node.getSUID())
                        .get(Constants.NodeColumns.ID, String.class)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Node to expand from not found"));

        fromNodeView = networkView.getNodeView(fromNode);

        // Expand network
        List<RawJson> expandDocs = connectionManager.execute(this.connectionId, expandQuery);
        List<CyNode> newNodes = networkManager.expandNetwork(expandDocs, networkView, fromNodeView, database,
                new ArangoNetworkMetadata.NodeExpansionMetadata(Helper.EXPAND_NODE_ID, expandQuery, connectionId));

        // Verify results
        assertEquals(Helper.EXPECTED_NEW_NODE_IDS.size(), newNodes.size());
        assertEquals(network.getEdgeCount(), Helper.EXISTING_EDGES.size() + Helper.EXPECTED_NEW_EDGES.size());
        verify(cyNetworkViewManager, times(0)).addNetworkView(any(CyNetworkView.class));

        verify(cyLayoutAlgorithm, times(1)).createTaskIterator(
                any(CyNetworkView.class),
                or(any(Object.class), isNull(Object.class)),
                anySet(), or(isNull(String.class), anyString()));

        assertEquals(1, networkManager.getAllNetworkNames().size());
        assertTrue(networkManager.getAllNetworkNames().contains("imdb"));

        // Sort and compare node IDs
        List<String> expectedNodeIds = Helper.EXPECTED_NEW_NODE_IDS.stream().sorted().collect(Collectors.toList());
        List<String> actualNodeIds = newNodes.stream()
                .map(node -> network.getDefaultNodeTable().getRow(node.getSUID()).get(Constants.NodeColumns.ID,
                        String.class))
                .sorted()
                .collect(Collectors.toList());

        assertEquals(expectedNodeIds, actualNodeIds);
    }
}