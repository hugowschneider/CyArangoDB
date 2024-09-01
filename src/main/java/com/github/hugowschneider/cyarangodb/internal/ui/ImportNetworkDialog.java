package com.github.hugowschneider.cyarangodb.internal.ui;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;

public class ImportNetworkDialog extends JDialog {
    private ConnectionManager connectionManager;
    private JComboBox<String> connectionDropdown;
    private JTextArea queryTextArea;
    private JTextField errorTextField;
    private JList<String> historyList;
    private DefaultListModel<String> historyListModel;

    public ImportNetworkDialog(ConnectionManager connectionManager, JFrame parentFrame) {
        super(parentFrame);
        this.connectionManager = connectionManager;
        setTitle("Import Network");
        setSize(400, 300);
        setLayout(new BorderLayout());
        setModalityType(ModalityType.APPLICATION_MODAL);
        setLocationRelativeTo(parentFrame);

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // First tab: Connection, Query, and Error
        JPanel queryPanel = new JPanel(new BorderLayout());

        try {
            URL iconURL = getClass().getClassLoader().getResource("arangodb_icon.png");
            if (iconURL != null) {
                Image icon = ImageIO.read(iconURL);
                setIconImage(icon);
            } else {
                System.err.println("Icon resource not found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Connection dropdown
        connectionDropdown = new JComboBox<>();
        for (String connectionName : connectionManager.getAllConnections().keySet()) {
            connectionDropdown.addItem(connectionName);
        }
        connectionDropdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateHistoryList();
            }
        });
        queryPanel.add(connectionDropdown, BorderLayout.NORTH);

        // Query textarea
        queryTextArea = new JTextArea(10, 30);
        JScrollPane queryScrollPane = new JScrollPane(queryTextArea);
        queryPanel.add(queryScrollPane, BorderLayout.CENTER);

        // Error text field
        errorTextField = new JTextField();
        errorTextField.setEditable(false);
        queryPanel.add(errorTextField, BorderLayout.SOUTH);

        // Add first tab
        tabbedPane.addTab("Query", queryPanel);

        // Second tab: History of executed queries
        JPanel historyPanel = new JPanel(new BorderLayout());

        // History list
        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane historyScrollPane = new JScrollPane(historyList);
        historyPanel.add(historyScrollPane, BorderLayout.CENTER);

        // Add second tab
        tabbedPane.addTab("History", historyPanel);

        // Add tabbed pane to the dialog
        add(tabbedPane, BorderLayout.CENTER);

        // Add a button to execute the query and store it in history
        JButton executeButton = new JButton("Execute Query");
        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String query = queryTextArea.getText();
                String connectionName = (String) connectionDropdown.getSelectedItem();

                try {
                    // Delegate execution to ConnectionManager
                    connectionManager.execute(connectionName, query);

                    // Update the history list
                    updateHistoryList();

                    // Clear any previous error messages
                    errorTextField.setText("");
                } catch (Exception ex) {
                    // Display error message
                    errorTextField.setText("Error executing query: " + ex.getMessage());
                }
            }
        });
        queryPanel.add(executeButton, BorderLayout.SOUTH);

        // Initialize the history list with existing history
        updateHistoryList();
    }

    private void updateHistoryList() {
        String connectionName = (String) connectionDropdown.getSelectedItem();
        List<ConnectionDetails.QueryHistory> history = connectionManager.getQueryHistory(connectionName);

        // Sort history by datetime in descending order
        Collections.sort(history, new Comparator<ConnectionDetails.QueryHistory>() {
            @Override
            public int compare(ConnectionDetails.QueryHistory o1, ConnectionDetails.QueryHistory o2) {
                return o2.getExecutedAt().compareTo(o1.getExecutedAt());
            }
        });

        // Update the history list model
        historyListModel.clear();
        for (ConnectionDetails.QueryHistory entry : history) {
            historyListModel.addElement(entry.getExecutedAt() + " - " + entry.getQuery());
        }
    }
}