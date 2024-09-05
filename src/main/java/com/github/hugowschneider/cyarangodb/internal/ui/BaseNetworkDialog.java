package com.github.hugowschneider.cyarangodb.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoDatabase;
import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.flex.AqlTokenMaker;
import com.github.hugowschneider.cyarangodb.internal.network.ImportNetworkException;
import com.github.hugowschneider.cyarangodb.internal.aql.AQLCompletionProvider;

/**
 * An abstract base class for dialogs that interact with ArangoDB networks.
 * Provides common functionality for executing queries and managing query history.
 */
public abstract class BaseNetworkDialog extends JDialog {
    /**
     * The connection manager responsible for managing database connections.
     */
    protected final ConnectionManager connectionManager;

    /**
     * The dropdown for selecting a connection.
     */
    protected final JComboBox<String> connectionDropdown;

    /**
     * The text area for entering AQL queries.
     */
    protected final RSyntaxTextArea queryTextArea;

    /**
     * The table for displaying query history.
     */
    protected final JTable historyTable;

    /**
     * The model for the history table.
     */
    protected final DefaultTableModel historyTableModel;

    /**
     * The completion provider for AQL auto-completion.
     */
    private AQLCompletionProvider completionProvider;

    /**
     * The logger for logging messages.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseNetworkDialog.class);

    /**
     * Constructs a new BaseNetworkDialog.
     *
     * @param connectionManager the connection manager
     * @param parentFrame       the parent frame
     * @param title             the title of the dialog
     */
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
        if (connectionManager.getAllConnections().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please configure a valid connection before trying to import a network or expand a node.",
                    "No Connections", JOptionPane.WARNING_MESSAGE);
            this.dispose();
        } else {
            for (String connectionName : connectionManager.getAllConnections().keySet()) {
                connectionDropdown.addItem(connectionName);
            }
            connectionDropdown.addActionListener(e -> {
                String connectionName = (String) connectionDropdown.getSelectedItem();
                updateHistoryList();
                this.completionProvider.setDatabase(connectionManager.getArangoDatabase(connectionName));
            });
        }

        connectionDropdown.addActionListener(e -> updateHistoryList());

        TokenMakerFactory factory = TokenMakerFactory.getDefaultInstance();
        ((AbstractTokenMakerFactory) factory).putMapping("text/aql", AqlTokenMaker.class.getName());

        queryTextArea = new RSyntaxTextArea(10, 30);
        queryTextArea.setSyntaxEditingStyle("text/aql");
        queryTextArea.setCodeFoldingEnabled(false);
        queryTextArea.setAntiAliasingEnabled(true);
        queryTextArea.setTabSize(2);
        queryTextArea.setEditable(true);
        queryTextArea.setEnabled(true);

        setupCodeStyles(queryTextArea);
        setupAutoCompletion(queryTextArea);

        RTextScrollPane scrollPane = new RTextScrollPane(queryTextArea);

        historyTableModel = new DefaultTableModel(new Object[] { "Executed At", "Query", "Run", "Copy", "Delete" }, 0);
        historyTable = new JTable(historyTableModel);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.getColumn("Run").setCellRenderer(new ButtonRenderer("Run"));
        historyTable.getColumn("Run").setCellEditor(new ButtonEditor(new JCheckBox(), "Run", e -> runHistory()));
        historyTable.getColumn("Copy").setCellRenderer(new ButtonRenderer("Copy"));
        historyTable.getColumn("Copy").setCellEditor(new ButtonEditor(new JCheckBox(), "Copy", e -> copyQuery()));
        historyTable.getColumn("Delete").setCellRenderer(new ButtonRenderer("Delete"));
        historyTable.getColumn("Delete")
                .setCellEditor(new ButtonEditor(new JCheckBox(), "Delete", e -> deleteHistory()));

        // Setup UI
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(connectionDropdown, BorderLayout.NORTH);

        Component topComponent = renderTopComponent();
        if (topComponent != null) {
            topPanel.add(topComponent, BorderLayout.CENTER);
        }

        add(topPanel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel queryPanel = new JPanel(new BorderLayout());
        Component centerComponent = renderCenterComponent();
        if (centerComponent != null) {
            queryPanel.add(centerComponent, BorderLayout.NORTH);
        }
        queryPanel.add(scrollPane, BorderLayout.CENTER);
        tabbedPane.addTab("Query", queryPanel);

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        tabbedPane.addTab("History", historyPanel);

        add(tabbedPane, BorderLayout.CENTER);

        JButton executeButton = new JButton(getExecuteButtonLabel());
        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeQuery();
            }
        });
        add(executeButton, BorderLayout.SOUTH);

        // Initialize the history list with existing history
        updateHistoryList();
        // Initialize the completion provider with the first connection
        this.completionProvider
                .setDatabase(connectionManager.getArangoDatabase((String) connectionDropdown.getSelectedItem()));
    }

    /**
     * Sets up the code styles for the query text area.
     *
     * @param queryTextArea the query text area
     */
    private void setupCodeStyles(RSyntaxTextArea queryTextArea) {
        Theme theme;
        try {
            URL themeURL = getClass().getClassLoader().getResource("theme.xml");
            InputStream in = themeURL.openStream();
            theme = Theme.load(in);
            in.close();
            theme.apply(queryTextArea);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Renders the top component of the dialog.
     *
     * @return the top component
     */
    protected abstract Component renderTopComponent();

    /**
     * Renders the center component of the dialog.
     *
     * @return the center component
     */
    protected abstract Component renderCenterComponent();

    /**
     * Returns the label for the execute button.
     *
     * @return the execute button label
     */
    protected String getExecuteButtonLabel() {
        return "Import";
    }

    /**
     * Sets up auto-completion for the query text area.
     *
     * @param textArea the query text area
     */
    protected void setupAutoCompletion(RSyntaxTextArea textArea) {
        this.completionProvider = new AQLCompletionProvider(null);

        AutoCompletion ac = new AutoCompletion(completionProvider);

        ac.install(textArea);
    }

    /**
     * Processes the query result.
     *
     * @param docs     the list of RawJson documents
     * @param database the ArangoDatabase instance
     * @param query    the query string
     * @throws ImportNetworkException if an error occurs during network import
     */
    protected abstract void processQueryResult(List<RawJson> docs, ArangoDatabase database, String query)
            throws ImportNetworkException;

    /**
     * Executes the query entered in the query text area.
     */
    protected void executeQuery() {
        String query = queryTextArea.getText();
        String connectionName = (String) connectionDropdown.getSelectedItem();
        JDialog waitDialog = createWaitDialog();

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    List<RawJson> docs = connectionManager.execute(connectionName, query);
                    if (docs.isEmpty()) {
                        throw new ImportNetworkException("No results found for query");
                    }
                    processQueryResult(docs, connectionManager.getArangoDatabase(connectionName), query);
                    updateHistoryList();
                    BaseNetworkDialog.this.dispose();
                } catch (ImportNetworkException e) {
                    JOptionPane.showMessageDialog(BaseNetworkDialog.this, e.getMessage(), "Error Importing Network",
                            JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    JOptionPane.showMessageDialog(BaseNetworkDialog.this, e.getMessage(), "Error executing query",
                            JOptionPane.ERROR_MESSAGE);
                }
                return null;
            }

            @Override
            protected void done() {
                waitDialog.dispose();
            }
        };

        worker.execute();
        waitDialog.setVisible(true);
    }

    /**
     * Updates the history list with the latest query history.
     */
    protected void updateHistoryList() {
        String connectionName = (String) connectionDropdown.getSelectedItem();
        List<ConnectionDetails.QueryHistory> history = connectionManager.getQueryHistory(connectionName);

        Collections.sort(history, Comparator.comparing(ConnectionDetails.QueryHistory::getExecutedAt).reversed());

        historyTableModel.setRowCount(0);

        for (ConnectionDetails.QueryHistory entry : history) {
            historyTableModel.addRow(new Object[] { entry.getExecutedAt(), entry.getQuery(), "Run", "Copy", "Delete" });
        }
    }

    /**
     * Creates a wait dialog to display while the query is being processed.
     *
     * @return the wait dialog
     */
    private JDialog createWaitDialog() {
        JDialog waitDialog = new JDialog(this, "Please Wait", true);
        waitDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        waitDialog.setSize(300, 100);
        waitDialog.setLocationRelativeTo(this);
        waitDialog.add(new JLabel("Processing query, please wait...", JLabel.CENTER));
        return waitDialog;
    }

    /**
     * Runs the selected query from the history list.
     */
    protected void runHistory() {
        int row = historyTable.getSelectedRow();
        String connectionName = (String) connectionDropdown.getSelectedItem();

        // Create and display the wait dialog
        JDialog waitDialog = createWaitDialog();

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    List<RawJson> docs = connectionManager.runHistory(connectionName, row);
                    processQueryResult(docs, connectionManager.getArangoDatabase(connectionName),
                            connectionManager.getQueryHistory(connectionName).get(row).getQuery());
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(BaseNetworkDialog.this, ex.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                waitDialog.dispose();
                BaseNetworkDialog.this.dispose();
            }
        };

        worker.execute();
        waitDialog.setVisible(true);
    }

    /**
     * Copies the selected query from the history list to the query text area.
     */
    protected void copyQuery() {
        int row = historyTable.getSelectedRow();
        String query = (String) historyTableModel.getValueAt(row, 1);
        queryTextArea.setText(query);
    }

    /**
     * Deletes the selected query from the history list.
     */
    protected void deleteHistory() {
        int row = historyTable.getSelectedRow();
        String connectionName = (String) connectionDropdown.getSelectedItem();
        connectionManager.deleteQueryHistory(connectionName, row);
        updateHistoryList();
    }
}