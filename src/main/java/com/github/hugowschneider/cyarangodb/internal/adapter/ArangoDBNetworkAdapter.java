package com.github.hugowschneider.cyarangodb.internal.adapter;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.BaseDocument;

public class ArangoDBNetworkAdapter {
    private CyNetworkFactory networkFactory;
    private CyNetworkManager networkManager;

    public ArangoDBNetworkAdapter(CyNetworkFactory networkFactory, CyNetworkManager networkManager) {
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
    }

    public CyNetwork adapt(ArangoCursor<BaseDocument> documents) {
        CyNetwork network = networkFactory.createNetwork();
        network.getRow(network).set(CyNetwork.NAME, "Imported From ArangoDB");
        CyTable nodeTable = network.getDefaultNodeTable();

        documents.forEach(doc -> {
            CyNode node = network.addNode();
            CyRow row = nodeTable.getRow(node.getSUID());

            for (String property : doc.getProperties().keySet()) {
                Object value = doc.getProperties().get(property);
                row.set(property, value);
            }

        });

        return network;

    }

}
