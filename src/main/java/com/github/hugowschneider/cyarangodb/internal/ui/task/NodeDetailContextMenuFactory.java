package com.github.hugowschneider.cyarangodb.internal.ui.task;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNode;
import org.cytoscape.task.AbstractNodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.ui.DetailDialog;

/**
 * A factory for creating context menu tasks that display node details.
 */
public class NodeDetailContextMenuFactory extends AbstractNodeViewTaskFactory {

    /**
     * The connection manager responsible for managing database connections.
     */
    private final ConnectionManager connectionManager;

    /**
     * The parent frame of the application.
     */
    private final JFrame parentFrame;

    /**
     * Constructs a new NodeDetailContextMenuFactory.
     *
     * @param connectionManager the connection manager
     * @param parentFrame       the parent frame
     */
    public NodeDetailContextMenuFactory(ConnectionManager connectionManager, JFrame parentFrame) {
        this.connectionManager = connectionManager;
        this.parentFrame = parentFrame;
    }

    /**
     * Creates a task iterator for displaying node details.
     *
     * @param nodeView    the view of the node
     * @param networkView the view of the network containing the node
     * @return a TaskIterator for displaying node details
     */
    @Override
    public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView networkView) {
        SwingUtilities.invokeLater(() -> {
            DetailDialog dialog = new DetailDialog(connectionManager, parentFrame, nodeView.getModel(),
                    networkView.getModel());
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