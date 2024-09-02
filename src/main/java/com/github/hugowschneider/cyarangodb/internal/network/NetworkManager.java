package com.github.hugowschneider.cyarangodb.internal.network;

import com.arangodb.ArangoDatabase;
import com.arangodb.util.RawJson;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import java.util.UUID;
import java.util.stream.Collectors;

public class NetworkManager {
    private CyNetworkFactory networkFactory;
    private CyNetworkManager networkManager;
    private CyNetworkViewFactory networkViewFactory;
    private CyNetworkViewManager networkViewManager;
    private CyApplicationManager applicationManager;
    private CyLayoutAlgorithmManager layoutAlgorithmManager;
    private Map<String, NetworkContext> networks;
    private TaskManager<?, ?> taskManager;

    public NetworkManager(CyNetworkFactory networkFactory, CyNetworkManager networkManager,
            CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager,
            CyApplicationManager applicationManager, CyLayoutAlgorithmManager layoutAlgorithmManager,
            TaskManager<?, ?> taskManager) {
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.applicationManager = applicationManager;
        this.layoutAlgorithmManager = layoutAlgorithmManager;
        this.taskManager = taskManager;
        this.networks = new HashMap<>();
    }

    public void handleNetworkView(CyNetwork network) {
        CyNetworkView view = networkViewFactory.createNetworkView(network);
        networkViewManager.addNetworkView(view);
        applicationManager.setCurrentNetworkView(view);
        ArangoNetworkStyle.applyStyles(networkViewManager);
        applicationManager.setCurrentNetwork(network);

        CyLayoutAlgorithm layoutAlgorithm = this.layoutAlgorithmManager.getDefaultLayout();
        TaskIterator taskIterator = layoutAlgorithm.createTaskIterator(view, layoutAlgorithm.getDefaultLayoutContext(),
                view.getNodeViews().stream().collect(Collectors.toSet()),
                null);
        taskManager.execute(taskIterator);
    }

    private void addNetwork(CyNetwork network, ArangoNetworkAdapter adapter) {
        String uuid = UUID.randomUUID().toString();
        network.getDefaultNetworkTable().getRow(network.getSUID()).set("uuid", uuid);
        networks.put(uuid, new NetworkContext(network, adapter));
    }

    public NetworkImportResult importNetwork(List<RawJson> docs, ArangoDatabase database, String query)
            throws ImportNetworkException {
        QueryResultValidator validator = new QueryResultValidator(docs);
        if (!validator.isEdgeList() && !validator.isPathList()) {
            throw new ImportNetworkException(
                    "The result of the query must be either a list of edges or a list of paths.");
        }

        ArangoNetworkAdapter adapter = new ArangoNetworkAdapter(database, networkFactory);
        CyNetwork network;
        if (validator.isEdgeList()) {
            network = adapter.adaptEdges(docs);
        } else {
            network = adapter.adaptPaths(docs);
        }
        CyTable networkTable = network.getDefaultNetworkTable();
        networkTable.createColumn("query", String.class, false);
        networkTable.createColumn("uuid", String.class, false);

        network.getDefaultNetworkTable().getRow(network.getSUID()).set("query", query);
        networkManager.addNetwork(network);
        addNetwork(network, adapter);
        handleNetworkView(network);

        return new NetworkImportResult(network.getNodeCount(), network.getEdgeCount());
    }

    public NetworkImportResult expandNetwork(List<RawJson> docs, CyNetworkView networkView, View<CyNode> fromNodeView,
            ArangoDatabase database, String query) throws ImportNetworkException {
        QueryResultValidator validator = new QueryResultValidator(docs);
        if (!validator.isEdgeList() && !validator.isPathList()) {
            throw new ImportNetworkException(
                    "The result of the query must be either a list of edges or a list of paths.");
        }
        String nodeId = networkView.getModel().getDefaultNodeTable().getRow(fromNodeView.getModel().getSUID()).get(
                "Id",
                String.class);
        if (!validator.isNodeEdgePresent(nodeId)) {
            throw new ImportNetworkException(String.format(
                    "The result does not contain an edge to the selected node '%1$s'.", nodeId));
        }
        CyNetwork network = networkView.getModel();
        NetworkContext networkContext = this.networks
                .get(network.getDefaultNetworkTable().getRow(network.getSUID()).get("uuid",
                        String.class));
        List<CyNode> newNodes;
        if (validator.isPathList()) {
            newNodes = networkContext.getAdapter().expandWithPath(docs, nodeId);
        } else {
            newNodes = networkContext.getAdapter().expandWithEdges(docs, nodeId);
        }

        List<Long> suids = newNodes.stream().map((node) -> {
            return node.getSUID();
        }).collect(Collectors.toList());
        CyLayoutAlgorithm layoutAlgorithm = this.layoutAlgorithmManager.getDefaultLayout();
        TaskIterator taskIterator = layoutAlgorithm.createTaskIterator(networkView,
                layoutAlgorithm.getDefaultLayoutContext(),
                networkView.getNodeViews().stream().filter((node) -> {
                    return suids.contains(node.getModel().getSUID());
                }).collect(Collectors.toSet()),
                null);
        taskManager.execute(taskIterator);

        return new NetworkImportResult(newNodes.size(), 0);

    }

    private class NetworkContext {
        CyNetwork network;
        ArangoNetworkAdapter adapter;

        public NetworkContext(CyNetwork network, ArangoNetworkAdapter adapter) {
            this.network = network;
            this.adapter = adapter;
        }

        public CyNetwork getNetwork() {
            return network;
        }

        public ArangoNetworkAdapter getAdapter() {
            return adapter;
        }

    }
}