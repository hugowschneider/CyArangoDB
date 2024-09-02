package com.github.hugowschneider.cyarangodb.internal.ui;

import org.cytoscape.application.swing.AbstractCyAction;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;

public class ManageConnectionsAction extends AbstractCyAction {
    private ConnectionManager connectionManager;
    private JFrame mainFrame;

    public ManageConnectionsAction(ConnectionManager connectionManager, JFrame mainFrame) {
        super("Manage Connections");
        this.mainFrame = mainFrame;
        this.connectionManager = connectionManager;
        setPreferredMenu("Apps.ArangoDB");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ManageConnectionsDialog window = new ManageConnectionsDialog(this.connectionManager, mainFrame);
        window.setVisible(true);
    }
}