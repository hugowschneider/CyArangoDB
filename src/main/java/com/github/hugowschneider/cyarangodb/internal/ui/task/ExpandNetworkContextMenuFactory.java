package com.github.hugowschneider.cyarangodb.internal.ui.task;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.model.CyNode;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkManager;
import com.github.hugowschneider.cyarangodb.internal.ui.ExpandNetworkDialog;

/**
 * A factory for creating context menu tasks that expand a node in a network.
 */
public class ExpandNetworkContextMenuFactory extends AbstractNetworkViewTaskFactory {

    /**
     * The connection manager responsible for managing database connections.
     */
    private final ConnectionManager connectionManager;

    /**
     * The network manager responsible for network operations.
     */
    private final NetworkManager networkManager;

    /**
     * The parent frame of the application.
     */
    private final JFrame parentFrame;

    /**
     * Constructs a new ExpandNodeContextMenuFactory.
     *
     * @param networkManager    the network manager
     * @param connectionManager the connection manager
     * @param parentFrame       the parent frame
     */
    public ExpandNetworkContextMenuFactory(NetworkManager networkManager,
            ConnectionManager connectionManager,
            JFrame parentFrame) {
        this.connectionManager = connectionManager;
        this.networkManager = networkManager;
        this.parentFrame = parentFrame;
    }

    /**
     * Creates a task iterator for expanding a node.
     *
     * @param networkView the view of the network containing the node
     * @return a TaskIterator for expanding the node
     */
    @Override
    public TaskIterator createTaskIterator(CyNetworkView networkView) {
        SwingUtilities.invokeLater(() -> {
            ExpandNetworkDialog dialog = new ExpandNetworkDialog(connectionManager, networkManager, parentFrame,
                    networkView);
            dialog.setVisible(true);
        });
        return new TaskIterator(new AbstractTask() {
            @Override
            public void run(TaskMonitor arg0) throws Exception {
                // No additional tasks to run
            }
        });
    }
}