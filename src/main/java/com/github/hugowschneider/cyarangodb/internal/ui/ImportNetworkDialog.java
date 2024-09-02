package com.github.hugowschneider.cyarangodb.internal.ui;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

import org.fife.ui.rtextarea.RTextScrollPane;

import com.arangodb.ArangoDatabase;
import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.network.ArangoNetworkAdapter;
import com.github.hugowschneider.cyarangodb.internal.network.ImportNetworkException;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkImportResult;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkManager;

public class ImportNetworkDialog extends BaseNetworkDialog {

    private NetworkManager networkManager;

    public ImportNetworkDialog(ConnectionManager connectionManager, NetworkManager networkManager, JFrame parentFrame) {
        super(connectionManager, parentFrame, "Import Network");
        this.networkManager = networkManager;
    }

    @Override
    protected void setupTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Setup Query tab
        JPanel queryPanel = new JPanel(new BorderLayout());
        queryTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        queryPanel.add(new RTextScrollPane(queryTextArea), BorderLayout.CENTER);

        JButton executeButton = new JButton("Execute Query");
        executeButton.addActionListener(e -> executeQuery());
        queryPanel.add(executeButton, BorderLayout.PAGE_END);

        tabbedPane.addTab("Query", queryPanel);

        // Setup History tab
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.getColumn("Run").setCellRenderer(new ButtonRenderer("Run"));
        historyTable.getColumn("Run").setCellEditor(new ButtonEditor(new JCheckBox(), "Run", e -> runHistory()));
        historyTable.getColumn("Copy").setCellRenderer(new ButtonRenderer("Copy"));
        historyTable.getColumn("Copy").setCellEditor(new ButtonEditor(new JCheckBox(), "Copy", e -> copyQuery()));
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

        NetworkImportResult result = networkManager.importNetwork(docs, database, query);
        JOptionPane.showMessageDialog(this,
                String.format("Network imported with %1$d nodes and %2$d edges", result.getNodeCount(),
                        result.getEdgeCount()));

    }
}