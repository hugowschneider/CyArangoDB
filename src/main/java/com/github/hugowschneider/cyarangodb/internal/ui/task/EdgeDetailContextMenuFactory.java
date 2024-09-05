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

public class EdgeDetailContextMenuFactory extends AbstractEdgeViewTaskFactory {

    private final ConnectionManager connectionManager;
    private final JFrame parentFrame;

    public EdgeDetailContextMenuFactory(
            ConnectionManager connectionManager,
            JFrame parentFrame) {
        this.connectionManager = connectionManager;
        this.parentFrame = parentFrame;
    }

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

            }
        });
    }
}