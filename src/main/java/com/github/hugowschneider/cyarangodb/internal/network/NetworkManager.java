package com.github.hugowschneider.cyarangodb.internal.network;

import com.arangodb.ArangoDatabase;
import com.arangodb.util.RawJson;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.RenderingEngine;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class NetworkManager {
    private CyNetworkFactory networkFactory;
    private CyNetworkManager networkManager;
    private CyNetworkViewFactory networkViewFactory;
    private CyNetworkViewManager networkViewManager;
    private CyApplicationManager applicationManager;
    private Map<String, CyNetwork> networks;

    public NetworkManager(CyNetworkFactory networkFactory, CyNetworkManager networkManager,
            CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager,
            CyApplicationManager applicationManager) {
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.applicationManager = applicationManager;
        this.networks = new HashMap<>();
    }

    public void handleNetworkView(CyNetwork network) {
        CyNetworkView view = networkViewFactory.createNetworkView(network);
        networkViewManager.addNetworkView(view);
        applicationManager.setCurrentNetworkView(view);
        ArangoNetworkStyle.applyStyles(networkViewManager);
        applicationManager.setCurrentNetwork(network);
    }

    private void addNetwork(CyNetwork network) {
        String uuid = UUID.randomUUID().toString();
        network.getDefaultNetworkTable().getRow(network.getSUID()).set("uuid", uuid);
        networks.put(uuid, network);
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
        addNetwork(network);
        handleNetworkView(network);

        return new NetworkImportResult(network.getNodeCount(), network.getEdgeCount());
    }
}