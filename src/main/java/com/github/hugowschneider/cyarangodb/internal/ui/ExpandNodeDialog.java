package com.github.hugowschneider.cyarangodb.internal.ui;

import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.JTextArea;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.arangodb.ArangoDatabase;
import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.network.ImportNetworkException;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkManager;
import javax.swing.JOptionPane;

public class ExpandNodeDialog extends BaseNetworkDialog {
        private View<CyNode> nodeView;
        private CyNetworkView networkView;
        private NetworkManager networkManager;
        private JEditorPane instructionText;
        private Font currentFont;

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
                String fontFamily = currentFont.getFamily();
                int fontSize = currentFont.getSize();
                String htmlContent = String.format(
                                "<html><body style=\"font-family:%s; font-size:%dpt;margin: 10px;\">" +
                                                "To expand the selected node, make sure to include at least one edge to the node with <code>_id</code> = <b><code>%s</code></b></body></html>",
                                fontFamily, fontSize, nodeId);

                instructionText.setText(htmlContent);

        }

        @Override
        protected void setupTabbedPane() {

                JTabbedPane tabbedPane = new JTabbedPane();

                // Setup Query tab
                JPanel queryPanel = new JPanel(new BorderLayout());
                // Add instruction text
                instructionText = new JEditorPane();
                instructionText.setContentType("text/html");

                // Retrieve the current font from the queryPanel
                currentFont = new Font("Monospaced", Font.PLAIN, 14);

                instructionText.setFont(currentFont);
                instructionText.setEditable(false);
                instructionText.setBackground(queryPanel.getBackground());
                queryPanel.add(instructionText, BorderLayout.NORTH);

                queryTextArea.setFont(currentFont);
                queryPanel.add(new RTextScrollPane(queryTextArea), BorderLayout.CENTER);

                JButton executeButton = new JButton("Execute Query");
                executeButton.addActionListener(e -> executeQuery());
                queryPanel.add(executeButton, BorderLayout.PAGE_END);

                tabbedPane.addTab("Query", queryPanel);

                // Setup History tab
                JPanel historyPanel = new JPanel(new BorderLayout());
                historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                historyTable.getColumn("Run").setCellRenderer(new ButtonRenderer("Run"));
                historyTable.getColumn("Run")
                                .setCellEditor(new ButtonEditor(new JCheckBox(), "Run", e -> runHistory()));
                historyTable.getColumn("Copy").setCellRenderer(new ButtonRenderer("Copy"));
                historyTable.getColumn("Copy")
                                .setCellEditor(new ButtonEditor(new JCheckBox(), "Copy", e -> copyQuery()));
                historyTable.getColumn("Delete").setCellRenderer(new ButtonRenderer("Delete"));
                historyTable.getColumn("Delete")
                                .setCellEditor(new ButtonEditor(new JCheckBox(), "Delete", e -> deleteHistory()));
                historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

                tabbedPane.addTab("History", historyPanel);

                add(tabbedPane, BorderLayout.CENTER);
        }

        @Override
        protected void processQueryResult(List<RawJson> docs, ArangoDatabase database, String query)
                        throws ImportNetworkException {
                // TODO Auto-generated method stub

        }
}