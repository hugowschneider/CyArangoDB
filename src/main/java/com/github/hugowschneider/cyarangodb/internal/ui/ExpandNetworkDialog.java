package com.github.hugowschneider.cyarangodb.internal.ui;

import java.awt.Component;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;

import com.arangodb.ArangoDatabase;
import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.network.ArangoNetworkMetadata;
import com.github.hugowschneider.cyarangodb.internal.network.ImportNetworkException;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkManager;

/**
 * A dialog for expanding a node in a network.
 */
public class ExpandNetworkDialog extends BaseNetworkDialog {

    /**
     * The view of the network containing the node.
     */
    private CyNetworkView networkView;

    /**
     * The manager responsible for network operations.
     */
    private NetworkManager networkManager;

    /**
     * Constructs a new ExpandNodeDialog.
     *
     * @param connectionManager the connection manager
     * @param networkManager    the network manager
     * @param parentFrame       the parent frame
     * @param networkView       the view of the network containing the node
     */
    public ExpandNetworkDialog(ConnectionManager connectionManager, NetworkManager networkManager, JFrame parentFrame,
            CyNetworkView networkView) {
        super(connectionManager, parentFrame, "Expand Network");
        this.networkView = networkView;
        this.networkManager = networkManager;
    }

    /**
     * Processes the query result and expands the network.
     *
     * @param docs     the list of RawJson documents
     * @param database the ArangoDatabase instance
     * @param metadata the metadata of the network
     * @throws ImportNetworkException if an error occurs during network import
     */
    @Override
    protected void processQueryResult(List<RawJson> docs, ArangoDatabase database, ArangoNetworkMetadata metadata)
            throws ImportNetworkException {

        List<CyNode> result = networkManager.expandNetwork(docs, networkView, database,
                new ArangoNetworkMetadata.NetworkExpansionMetadata(metadata.getQuery(),
                        metadata.getConnectionId()));
        JOptionPane.showMessageDialog(this,
                String.format("Network was expanded with %1$d nodes", result.size()));
    }

    /**
     * Renders the top component of the dialog.
     *
     * @return the top component, or null if there is no top component
     */
    @Override
    protected Component renderTopComponent() {
        return null;
    }

    /**
     * Renders the center component of the dialog.
     *
     * @return the center component
     */
    @Override
    protected Component renderCenterComponent() {
        return null;
    }
}