package com.github.hugowschneider.cyarangodb.internal.ui;

import org.cytoscape.application.swing.AbstractCyAction;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;

/**
 * Represents an action for managing connections in the UI.
 * Extends the {@link org.cytoscape.application.swing.AbstractCyAction} class.
 */
public class ManageConnectionsAction extends AbstractCyAction {
    /**
     * The connection manager responsible for managing database connections.
     */
    private ConnectionManager connectionManager;

    /**
     * The main frame of the application.
     */
    private JFrame mainFrame;

    /**
     * Constructs a new action for managing connections.
     *
     * @param connectionManager the connection manager
     * @param mainFrame         the main frame
     */
    public ManageConnectionsAction(ConnectionManager connectionManager, JFrame mainFrame) {
        super("Manage Connections");
        this.mainFrame = mainFrame;
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
        ManageConnectionsDialog window = new ManageConnectionsDialog(this.connectionManager, mainFrame);
        window.setVisible(true);
    }
}