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
    private Map<String, ArangoNetworkAdapter> networks;
    private TaskManager<?, ?> taskManager;
    private ArangoNetworkStyle arangoNetworkStyle;

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

    public List<String> getAllNetworkNames() {
        return networkManager.getNetworkSet().stream().map((n) -> n.getDefaultNetworkTable().getRow(n.getSUID()))
                .map(row -> row.get("name", String.class))
                .collect(Collectors.toList());
    }

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

    private void addNetwork(CyNetwork network, ArangoNetworkAdapter adapter) {
        String uuid = UUID.randomUUID().toString();
        network.getDefaultNetworkTable().getRow(network.getSUID()).set("uuid", uuid);
        networks.put(uuid, adapter);
    }

    public NetworkImportResult importNetwork(List<RawJson> docs, ArangoDatabase database, String networkName,
            String query)
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
        network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", networkName);
        networkManager.addNetwork(network);
        addNetwork(network, adapter);
        handleNetworkView(network, null);

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
        ArangoNetworkAdapter adapter = this.networks
                .get(network.getDefaultNetworkTable().getRow(network.getSUID()).get("uuid",
                        String.class));
        List<CyNode> newNodes;
        if (validator.isPathList()) {
            newNodes = adapter.expandWithPath(docs, nodeId);
        } else {
            newNodes = adapter.expandWithEdges(docs, nodeId);
        }
        networkView.updateView();
        handleNetworkView(network, networkView);

        return new NetworkImportResult(newNodes.size(), 0);

    }

    private void applyLayout(CyNetworkView networkView, Set<View<CyNode>> nodes) {
        CyLayoutAlgorithm layoutAlgorithm = this.layoutAlgorithmManager.getDefaultLayout();
        TaskIterator taskIterator = layoutAlgorithm.createTaskIterator(networkView,
                layoutAlgorithm.getDefaultLayoutContext(),
                nodes,
                null);
        taskManager.execute(taskIterator);
    }
}