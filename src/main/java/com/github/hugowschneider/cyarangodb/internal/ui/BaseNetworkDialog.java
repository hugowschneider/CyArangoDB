package com.github.hugowschneider.cyarangodb.internal.ui;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;

import com.arangodb.ArangoDatabase;
import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;

import com.github.hugowschneider.cyarangodb.internal.ui.aql.AQLCompletionProvider;
import com.github.hugowschneider.cyarangodb.internal.flex.AqlTokenMaker;
import com.github.hugowschneider.cyarangodb.internal.network.ImportNetworkException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseNetworkDialog extends JDialog {
    protected final ConnectionManager connectionManager;
    protected final JComboBox<String> connectionDropdown;
    protected final RSyntaxTextArea queryTextArea;
    protected final JTable historyTable;
    protected final DefaultTableModel historyTableModel;

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseNetworkDialog.class);

    public BaseNetworkDialog(ConnectionManager connectionManager, JFrame parentFrame, String title) {
        super(parentFrame, title, true);
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
        ((AbstractTokenMakerFactory) factory).putMapping("text/aql", AqlTokenMaker.class.getName());

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

    protected abstract void setupTabbedPane();

    protected void setupAutoCompletion(RSyntaxTextArea textArea) {
        CompletionProvider provider = createCompletionProvider();
        AutoCompletion ac = new AutoCompletion(provider);
        ac.install(textArea);
    }

    protected CompletionProvider createCompletionProvider() {
        // Add SQL keywords for autocompletion
        String[] keywords = { "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER",
                "JOIN" };
        CompletionProvider provider = new AQLCompletionProvider(keywords);

        return provider;
    }

    protected abstract void processQueryResult(List<RawJson> docs, ArangoDatabase database, String query)
            throws ImportNetworkException;

    protected void executeQuery() {
        String query = queryTextArea.getText();
        String connectionName = (String) connectionDropdown.getSelectedItem();

        try {
            List<RawJson> docs = connectionManager.execute(connectionName, query);
            processQueryResult(docs, connectionManager.getArangoDatabase(connectionName), query);
            updateHistoryList();
            this.dispose();
        } catch (ImportNetworkException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error Importing Network", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error executing query", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void updateHistoryList() {
        String connectionName = (String) connectionDropdown.getSelectedItem();
        List<ConnectionDetails.QueryHistory> history = connectionManager.getQueryHistory(connectionName);

        Collections.sort(history, Comparator.comparing(ConnectionDetails.QueryHistory::getExecutedAt).reversed());
        historyTableModel.setRowCount(0);

        for (ConnectionDetails.QueryHistory entry : history) {
            historyTableModel.addRow(new Object[] { entry.getExecutedAt(), entry.getQuery(), "Run", "Copy", "Delete" });
        }
    }

    protected void runHistory() {
        int row = historyTable.getSelectedRow();
        String connectionName = (String) connectionDropdown.getSelectedItem();
        try {
            List<RawJson> docs = connectionManager.runHistory(connectionName, row);
            processQueryResult(docs, connectionManager.getArangoDatabase(connectionName),
                    connectionManager.getQueryHistory(connectionName).get(row).getQuery());
            this.dispose();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    protected void copyQuery() {
        int row = historyTable.getSelectedRow();
        String query = (String) historyTableModel.getValueAt(row, 1);
        queryTextArea.setText(query);
    }

    protected void deleteHistory() {
        int row = historyTable.getSelectedRow();
        String connectionName = (String) connectionDropdown.getSelectedItem();
        connectionManager.deleteQueryHistory(connectionName, row);
        updateHistoryList();
    }
}