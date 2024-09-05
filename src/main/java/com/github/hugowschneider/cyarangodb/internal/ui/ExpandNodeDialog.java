package com.github.hugowschneider.cyarangodb.internal.ui;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import com.arangodb.ArangoDatabase;
import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.network.ImportNetworkException;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkImportResult;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkManager;

/**
 * A dialog for expanding a node in a network.
 */
public class ExpandNodeDialog extends BaseNetworkDialog {
    /**
     * The view of the node to be expanded.
     */
    private View<CyNode> nodeView;

    /**
     * The view of the network containing the node.
     */
    private CyNetworkView networkView;

    /**
     * The manager responsible for network operations.
     */
    private NetworkManager networkManager;

    /**
     * The text area for displaying instructions.
     */
    private JEditorPane instructionText;

    /**
     * Constructs a new ExpandNodeDialog.
     *
     * @param connectionManager the connection manager
     * @param networkManager    the network manager
     * @param parentFrame       the parent frame
     * @param nodeView          the view of the node to be expanded
     * @param networkView       the view of the network containing the node
     */
    public ExpandNodeDialog(ConnectionManager connectionManager, NetworkManager networkManager, JFrame parentFrame,
                            View<CyNode> nodeView, CyNetworkView networkView) {
        super(connectionManager, parentFrame, "Expand Node");
        this.nodeView = nodeView;
        this.networkView = networkView;
        this.networkManager = networkManager;
        setupText();
    }

    /**
     * Sets up the instruction text for the dialog.
     * Displays a message if no node is selected.
     */
    private void setupText() {
        if (networkView == null || nodeView == null) {
            JOptionPane.showMessageDialog(this, "Select a node before choosing this action", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
            return;
        }
        String nodeId = networkView.getModel().getDefaultNodeTable().getRow(nodeView.getModel().getSUID()).get(
                "Id", String.class);

        String htmlContent = String.format(
                "<html><body style=\"font-family:Verdana; font-size:14pt; margin: 10px;\">" +
                        "To expand the selected node, make sure to include at least one edge to the node with " +
                        "<code style=\"font-family:monospace;font-size:14pt;\">_id = <b>%s</b></code>" +
                        "</body></html>",
                nodeId);

        instructionText.setText(htmlContent);
    }

    /**
     * Processes the query result and expands the network.
     *
     * @param docs     the list of RawJson documents
     * @param database the ArangoDatabase instance
     * @param query    the query string
     * @throws ImportNetworkException if an error occurs during network import
     */
    @Override
    protected void processQueryResult(List<RawJson> docs, ArangoDatabase database, String query)
            throws ImportNetworkException {
        NetworkImportResult result = networkManager.expandNetwork(docs, networkView, nodeView, database, query);
        JOptionPane.showMessageDialog(this,
                String.format("Network was expanded with %1$d nodes", result.getNodeCount(), result.getEdgeCount()));
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
        instructionText = new JEditorPane();
        instructionText.setContentType("text/html");
        instructionText.setEditable(false);

        JButton copyButton = new JButton("Copy Node ID");
        copyButton.addActionListener(e -> {
            String nodeId = networkView.getModel().getDefaultNodeTable()
                    .getRow(nodeView.getModel().getSUID()).get("Id", String.class);
            StringSelection stringSelection = new StringSelection(nodeId);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });

        // Create a panel to hold the instructionText and the button
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(instructionText);
        panel.add(copyButton);

        return panel;
    }
}