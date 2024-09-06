package com.github.hugowschneider.cyarangodb.internal.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cytoscape.application.CyApplicationManager;
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
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;

import com.arangodb.ArangoDatabase;
import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.network.ArangoNetworkMetadata.NodeExpansionMetadata;

/**
 * Manages the creation, import, and expansion of networks in Cytoscape using
 * data from ArangoDB.
 */
public class NetworkManager {
    /**
     * Factory for creating Cytoscape networks.
     */
    private CyNetworkFactory networkFactory;

    /**
     * Manager for handling Cytoscape networks.
     */
    private CyNetworkManager networkManager;

    /**
     * Factory for creating Cytoscape network views.
     */
    private CyNetworkViewFactory networkViewFactory;

    /**
     * Manager for handling Cytoscape network views.
     */
    private CyNetworkViewManager networkViewManager;

    /**
     * Application manager for Cytoscape.
     */
    private CyApplicationManager applicationManager;

    /**
     * Manager for handling Cytoscape layout algorithms.
     */
    private CyLayoutAlgorithmManager layoutAlgorithmManager;

    /**
     * Map of network UUIDs to their corresponding ArangoNetworkAdapter.
     */
    private Map<String, ArangoNetworkAdapter> networks;

    /**
     * Task manager for executing tasks in Cytoscape.
     */
    private TaskManager<?, ?> taskManager;

    /**
     * Style manager for applying styles to networks.
     */
    private ArangoNetworkStyle arangoNetworkStyle;

    /**
     * Constructs a new NetworkManager.
     *
     * @param networkFactory         the factory for creating Cytoscape networks
     * @param networkManager         the manager for handling Cytoscape networks
     * @param networkViewFactory     the factory for creating Cytoscape network
     *                               views
     * @param networkViewManager     the manager for handling Cytoscape network
     *                               views
     * @param applicationManager     the application manager for Cytoscape
     * @param layoutAlgorithmManager the manager for handling Cytoscape layout
     *                               algorithms
     * @param taskManager            the task manager for executing tasks in
     *                               Cytoscape
     * @param arangoNetworkStyle     the style manager for applying styles to
     *                               networks
     */
    public NetworkManager(CyNetworkFactory networkFactory, CyNetworkManager networkManager,
            CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager,
            CyApplicationManager applicationManager, CyLayoutAlgorithmManager layoutAlgorithmManager,
            TaskManager<?, ?> taskManager, ArangoNetworkStyle arangoNetworkStyle) {
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.applicationManager = applicationManager;
        this.layoutAlgorithmManager = layoutAlgorithmManager;
        this.taskManager = taskManager;
        this.arangoNetworkStyle = arangoNetworkStyle;
        this.networks = new HashMap<>();
    }

    /**
     * Gets the names of all networks.
     *
     * @return a list of network names
     */
    public List<String> getAllNetworkNames() {
        return networkManager.getNetworkSet().stream().map((n) -> n.getDefaultNetworkTable().getRow(n.getSUID()))
                .map(row -> row.get("name", String.class))
                .collect(Collectors.toList());
    }

    /**
     * Handles the creation or updating of a network view.
     *
     * @param network     the network to create or update the view for
     * @param networkView the existing network view, or null to create a new one
     */
    public void handleNetworkView(CyNetwork network, CyNetworkView networkView) {
        CyNetworkView view;
        if (networkView != null) {
            view = networkView;
        } else {
            view = networkViewFactory.createNetworkView(network);
            networkViewManager.addNetworkView(view);
            applicationManager.setCurrentNetworkView(view);
            applicationManager.setCurrentNetwork(network);
        }
        arangoNetworkStyle.applyStyles(networkViewManager);
        applyLayout(view, view.getNodeViews().stream().collect(Collectors.toSet()));
    }

    /**
     * Adds a network to the manager and assigns it a UUID.
     *
     * @param network the network to add
     * @param adapter the adapter for the network
     */
    private void addNetwork(CyNetwork network, ArangoNetworkAdapter adapter) {
        String uuid = UUID.randomUUID().toString();
        network.getDefaultNetworkTable().getRow(network.getSUID()).set(Constants.NetworkColumns.ID, uuid);
        networks.put(uuid, adapter);
    }

    /**
     * Imports a network from a list of documents.
     *
     * @param docs        the list of RawJson documents
     * @param database    the ArangoDatabase instance
     * @param networkName the name of the network
     * @param metadata    the metadata of the network
     * @return the result of the network import
     * @throws ImportNetworkException if the import fails
     */
    public NetworkImportResult importNetwork(List<RawJson> docs, ArangoDatabase database, String networkName,
            ArangoNetworkMetadata metadata) throws ImportNetworkException {
        QueryResultValidator validator = new QueryResultValidator(docs);
        if (!validator.isEdgeList() && !validator.isPathList()) {
            throw new ImportNetworkException(
                    "The result of the query must be either a list of edges or a list of paths.");
        }

        ArangoNetworkAdapter adapter = new ArangoNetworkAdapter(database, networkFactory);
        CyNetwork network;
        if (validator.isEdgeList()) {
            network = adapter.importEdges(docs, metadata);
        } else {
            network = adapter.importPaths(docs, metadata);
        }

        network.getDefaultNetworkTable().getRow(network.getSUID()).set(Constants.NetworkColumns.NAME, networkName);
        networkManager.addNetwork(network);
        addNetwork(network, adapter);
        handleNetworkView(network, null);

        return new NetworkImportResult(network.getNodeCount(), network.getEdgeCount());
    }

    /**
     * Expands a network by adding nodes and edges from a list of documents.
     *
     * @param docs         the list of RawJson documents
     * @param networkView  the view of the network to expand
     * @param fromNodeView the view of the node to expand from
     * @param database     the ArangoDatabase instance
     * @param metadata     the metadata of the network
     * @return the result of the network expansion
     * @throws ImportNetworkException if the expansion fails
     */
    public NetworkImportResult expandNetwork(List<RawJson> docs, CyNetworkView networkView, View<CyNode> fromNodeView,
            ArangoDatabase database, ArangoNetworkMetadata.NodeExpansionMetadata metadata)
            throws ImportNetworkException {
        QueryResultValidator validator = new QueryResultValidator(docs);
        if (!validator.isEdgeList() && !validator.isPathList()) {
            throw new ImportNetworkException(
                    "The result of the query must be either a list of edges or a list of paths.");
        }

        if (!validator.isNodeEdgePresent(metadata.getNodeId())) {
            throw new ImportNetworkException(String.format(
                    "The result does not contain an edge to the selected node '%1$s'.", metadata.getNodeId()));
        }
        CyNetwork network = networkView.getModel();
        ArangoNetworkAdapter adapter = this.networks
                .get(network.getDefaultNetworkTable().getRow(network.getSUID()).get(Constants.NetworkColumns.ID,
                        String.class));
        List<CyNode> newNodes;
        if (validator.isPathList()) {
            newNodes = adapter.expandNodeWithPath(docs, metadata);
        } else {
            newNodes = adapter.expandNodeWithEdges(docs, metadata);
        }
        networkView.updateView();
        handleNetworkView(network, networkView);

        return new NetworkImportResult(newNodes.size(), 0);
    }

    /**
     * Applies a layout algorithm to a network view.
     *
     * @param networkView the network view to apply the layout to
     * @param nodes       the set of nodes to layout
     */
    private void applyLayout(CyNetworkView networkView, Set<View<CyNode>> nodes) {
        CyLayoutAlgorithm layoutAlgorithm = this.layoutAlgorithmManager.getDefaultLayout();
        TaskIterator taskIterator = layoutAlgorithm.createTaskIterator(networkView,
                layoutAlgorithm.getDefaultLayoutContext(),
                nodes,
                null);
        taskManager.execute(taskIterator);
    }

    /**
     * Expands a network by adding nodes and edges from a list of documents.
     *
     * @param docs        the list of RawJson documents
     * @param networkView the view of the network to expand
     * @param database    the ArangoDatabase instance
     * @param metadata    the metadata of the network
     * @return the list of new nodes
     * @throws ImportNetworkException if the expansion fails
     */
    public List<CyNode> expandNetwork(List<RawJson> docs, CyNetworkView networkView, ArangoDatabase database,
            ArangoNetworkMetadata.NetworkExpansionMetadata metadata) throws ImportNetworkException {

        QueryResultValidator validator = new QueryResultValidator(docs);
        if (!validator.isEdgeList() && !validator.isPathList()) {
            throw new ImportNetworkException(
                    "The result of the query must be either a list of edges or a list of paths.");
        }

        CyNetwork network = networkView.getModel();
        ArangoNetworkAdapter adapter = this.networks
                .get(network.getDefaultNetworkTable().getRow(network.getSUID()).get(Constants.NetworkColumns.ID,
                        String.class));
        List<CyNode> newNodes;
        if (validator.isEdgeList()) {
            newNodes = adapter.expandNetworkEdges(docs, metadata);
        } else {
            newNodes = adapter.expandNetworkPaths(docs, metadata);
        }

        networkView.updateView();
        handleNetworkView(network, networkView);

        return newNodes;
    }
}