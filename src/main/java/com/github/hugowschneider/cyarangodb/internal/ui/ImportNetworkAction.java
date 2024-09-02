package com.github.hugowschneider.cyarangodb.internal.ui;

import org.cytoscape.application.swing.AbstractCyAction;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkManager;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;

public class ImportNetworkAction extends AbstractCyAction {
    private ConnectionManager connectionManager;
    private NetworkManager networkManager;
    private JFrame mainFrame;

    public ImportNetworkAction(ConnectionManager connectionManager, NetworkManager networkManager, JFrame mainFrame) {
        super("Import Network");
        this.mainFrame = mainFrame;
        this.networkManager = networkManager;
        this.connectionManager = connectionManager;
        setPreferredMenu("Apps.ArangoDB");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImportNetworkDialog dialog = new ImportNetworkDialog(connectionManager, networkManager, mainFrame);
        dialog.setVisible(true);
    }
}