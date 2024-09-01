package com.github.hugowschneider.cyarangodb.internal.ui;

import org.cytoscape.application.swing.AbstractCyAction;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;

public class ImportNetworkAction extends AbstractCyAction {
    private ConnectionManager connectionManager;
    private JFrame mainFrame;

    public ImportNetworkAction(ConnectionManager connectionManager, JFrame mainFrame) {
        super("Import Network");
        this.mainFrame = mainFrame;
        this.connectionManager = connectionManager;
        setPreferredMenu("Apps.ArangoDB");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImportNetworkDialog dialog = new ImportNetworkDialog(connectionManager, mainFrame);
        dialog.setVisible(true);
    }
}