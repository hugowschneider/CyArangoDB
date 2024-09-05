package com.github.hugowschneider.cyarangodb.internal.ui;

import org.cytoscape.application.swing.AbstractCyAction;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkManager;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;

/**
 * Represents an action for importing a network from an ArangoDB database.
 * Extends the {@link org.cytoscape.application.swing.AbstractCyAction} class.
 */
public class ImportNetworkAction extends AbstractCyAction {
    /**
     * The connection manager responsible for managing database connections.
     */
    private ConnectionManager connectionManager;

    /**
     * The network manager responsible for network operations.
     */
    private NetworkManager networkManager;

    /**
     * The main frame of the application.
     */
    private JFrame mainFrame;

    /**
     * Constructs a new action for importing a network.
     *
     * @param connectionManager the connection manager
     * @param networkManager    the network manager
     * @param mainFrame         the main frame
     */
    public ImportNetworkAction(ConnectionManager connectionManager, NetworkManager networkManager, JFrame mainFrame) {
        super("Import Network");
        this.mainFrame = mainFrame;
        this.networkManager = networkManager;
        this.connectionManager = connectionManager;
        setPreferredMenu("Apps.ArangoDB");
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        ImportNetworkDialog dialog = new ImportNetworkDialog(connectionManager, networkManager, mainFrame);
        dialog.setVisible(true);
    }
}