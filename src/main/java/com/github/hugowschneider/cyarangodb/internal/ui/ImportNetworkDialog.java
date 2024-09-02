package com.github.hugowschneider.cyarangodb.internal.ui;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.connection.ImportNetworkException;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager.ImportResult;
import com.github.hugowschneider.cyarangodb.internal.ui.aql.AQLCompletionProvider;
import com.github.hugowschneider.cyarangodb.internal.flex.AqlTokenMaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportNetworkDialog extends JDialog {
    private final ConnectionManager connectionManager;
    private final JComboBox<String> connectionDropdown;
    private final RSyntaxTextArea queryTextArea;
    private final JTable historyTable;
    private final DefaultTableModel historyTableModel;

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportNetworkDialog.class);

    public ImportNetworkDialog(ConnectionManager connectionManager, JFrame parentFrame) {
        super(parentFrame, "Import Network", true);
        this.connectionManager = connectionManager;

        // Set dialog properties
        setLayout(new BorderLayout());
        setModalityType(ModalityType.APPLICATION_MODAL);
        setSize(1200, 600);
        setLocationRelativeTo(parentFrame);

        // Set icon image
        UIUtils.setIconImage(this, "arangodb_icon.png");

        // Initialize components
        connectionDropdown = new JComboBox<>();

        TokenMakerFactory factory = TokenMakerFactory.getDefaultInstance();
        ((AbstractTokenMakerFactory) factory).putMapping("text/aql",
                AqlTokenMaker.class.getName());

        queryTextArea = new RSyntaxTextArea(10, 30);
        queryTextArea.setSyntaxEditingStyle("text/aql");
        queryTextArea.setCodeFoldingEnabled(true);
        queryTextArea.setEditable(true);
        queryTextArea.setEnabled(true);
        setupAutoCompletion(queryTextArea);

        historyTableModel = new DefaultTableModel(new Object[] { "Executed At", "Query", "Run", "Copy", "Delete" }, 0);
        historyTable = new JTable(historyTableModel);

        // Setup UI
        setupConnectionPanel();
        setupTabbedPane();

        // Initialize the history list with existing history
        updateHistoryList();
    }

    private void setupConnectionPanel() {
        JPanel connectionPanel = new JPanel(new BorderLayout());
        for (String connectionName : connectionManager.getAllConnections().keySet()) {
            connectionDropdown.addItem(connectionName);
        }
        connectionDropdown.addActionListener(e -> updateHistoryList());
        connectionPanel.add(connectionDropdown, BorderLayout.NORTH);
        add(connectionPanel, BorderLayout.NORTH);
    }

    private void setupTabbedPane() {
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

    private void setupAutoCompletion(RSyntaxTextArea textArea) {
        CompletionProvider provider = createCompletionProvider();
        AutoCompletion ac = new AutoCompletion(provider);
        ac.install(textArea);
    }

    private CompletionProvider createCompletionProvider() {
        // Add SQL keywords for autocompletion
        String[] keywords = { "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER",
                "JOIN" };
        CompletionProvider provider = new AQLCompletionProvider(keywords);

        return provider;
    }

    private void executeQuery() {
        String query = queryTextArea.getText();
        String connectionName = (String) connectionDropdown.getSelectedItem();

        try {
            ImportResult result = connectionManager.execute(connectionName, query);
            updateHistoryList();
            JOptionPane.showMessageDialog(this,
                    String.format("Imported %1$d Nodes and %2$d Vertices", result.getVertexCount(),
                            result.getEdgeCount()));
            this.dispose();
        } catch (ImportNetworkException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this, "Error executing query: " + ex.getMessage());
        }
    }

    private void updateHistoryList() {
        String connectionName = (String) connectionDropdown.getSelectedItem();
        List<ConnectionDetails.QueryHistory> history = connectionManager.getQueryHistory(connectionName);

        Collections.sort(history, Comparator.comparing(ConnectionDetails.QueryHistory::getExecutedAt).reversed());
        historyTableModel.setRowCount(0);

        for (ConnectionDetails.QueryHistory entry : history) {
            historyTableModel.addRow(new Object[] { entry.getExecutedAt(), entry.getQuery(), "Run", "Copy", "Delete" });
        }
    }

    private void runHistory() {
        int row = historyTable.getSelectedRow();
        String connectionName = (String) connectionDropdown.getSelectedItem();
        try {
            ImportResult result = connectionManager.runHistory(connectionName, row);
            JOptionPane.showMessageDialog(this,
                    String.format("Imported %1$d Nodes and %2$d Vertices", result.getVertexCount(),
                            result.getEdgeCount()));
            this.dispose();

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }

    }

    private void copyQuery() {
        int row = historyTable.getSelectedRow();
        String query = (String) historyTableModel.getValueAt(row, 1);
        queryTextArea.setText(query);
    }

    private void deleteHistory() {
        int row = historyTable.getSelectedRow();
        String connectionName = (String) connectionDropdown.getSelectedItem();
        connectionManager.deleteQueryHistory(connectionName, row);
        updateHistoryList();
    }
}