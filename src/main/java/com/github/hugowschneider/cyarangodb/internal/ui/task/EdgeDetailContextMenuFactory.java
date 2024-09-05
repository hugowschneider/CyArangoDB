package com.github.hugowschneider.cyarangodb.internal.ui.task;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyEdge;
import org.cytoscape.task.AbstractEdgeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.ui.DetailDialog;

/**
 * A factory for creating context menu tasks that display edge details.
 */
public class EdgeDetailContextMenuFactory extends AbstractEdgeViewTaskFactory {

    /**
     * The connection manager responsible for managing database connections.
     */
    private final ConnectionManager connectionManager;

    /**
     * The parent frame of the application.
     */
    private final JFrame parentFrame;

    /**
     * Constructs a new EdgeDetailContextMenuFactory.
     *
     * @param connectionManager the connection manager
     * @param parentFrame       the parent frame
     */
    public EdgeDetailContextMenuFactory(ConnectionManager connectionManager, JFrame parentFrame) {
        this.connectionManager = connectionManager;
        this.parentFrame = parentFrame;
    }

    /**
     * Creates a task iterator for displaying edge details.
     *
     * @param edgeView    the view of the edge
     * @param networkView the view of the network containing the edge
     * @return a TaskIterator for displaying edge details
     */
    @Override
    public TaskIterator createTaskIterator(View<CyEdge> edgeView, CyNetworkView networkView) {
        SwingUtilities.invokeLater(() -> {
            DetailDialog dialog = new DetailDialog(connectionManager, parentFrame, edgeView.getModel(),
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