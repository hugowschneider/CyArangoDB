package com.github.hugowschneider.cyarangodb.internal.ui;

import java.awt.Component;
import java.awt.Font;
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

public class ExpandNodeDialog extends BaseNetworkDialog {
        private View<CyNode> nodeView;
        private CyNetworkView networkView;
        private NetworkManager networkManager;
        private JEditorPane instructionText;

        public ExpandNodeDialog(ConnectionManager connectionManager, NetworkManager networkManager, JFrame parentFrame,
                        View<CyNode> nodeView,
                        CyNetworkView networkView) {
                super(connectionManager, parentFrame, "Expand Node");
                this.nodeView = nodeView;
                this.networkView = networkView;
                this.networkManager = networkManager;
                setupText();

        }

        private void setupText() {
                if (networkView == null || nodeView == null) {
                        JOptionPane.showMessageDialog(this, "Select a node before choosing this action", "Info",
                                        JOptionPane.INFORMATION_MESSAGE);
                        this.dispose();
                        return;
                }
                String nodeId = networkView.getModel().getDefaultNodeTable().getRow(nodeView.getModel().getSUID()).get(
                                "Id",
                                String.class);

                String htmlContent = String.format(
                                "<html><body style=\"font-family:Verdana; font-size:14pt; margin: 10px;\">" +
                                                "To expand the selected node, make sure to include at least one edge to the node with "
                                                +
                                                "<code style=\"font-family:monospace;font-size:14pt;\">_id = <b>%s</b></code>"
                                                +
                                                "</body></html>",
                                nodeId);

                instructionText.setText(htmlContent);

        }

        @Override
        protected void processQueryResult(List<RawJson> docs, ArangoDatabase database, String query)
                        throws ImportNetworkException {

                NetworkImportResult result = networkManager.expandNetwork(docs, networkView, nodeView, database, query);
                JOptionPane.showMessageDialog(this,
                                String.format("Network was expanded with %1$d nodes", result.getNodeCount(),
                                                result.getEdgeCount()));

        }

        @Override
        protected Component renderTopComponent() {
                return null;
        }

        @Override
        protected Component renderCenterComponent() {
                instructionText = new JEditorPane();
                instructionText.setContentType("text/html");
                instructionText.setEditable(false);

                JButton copyButton = new JButton("Copy Node ID");
                copyButton.addActionListener(e -> {
                        String nodeId = networkView.getModel().getDefaultNodeTable()
                                        .getRow(nodeView.getModel().getSUID()).get(
                                                        "Id",
                                                        String.class);
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