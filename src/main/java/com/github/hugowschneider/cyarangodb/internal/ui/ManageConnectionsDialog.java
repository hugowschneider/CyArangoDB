package com.github.hugowschneider.cyarangodb.internal.ui;

import javax.imageio.ImageIO;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class ManageConnectionsDialog extends JDialog {
    static final Logger LOGGER = LoggerFactory.getLogger(ManageConnectionsDialog.class);

    private JTable connectionTable;
    private DefaultTableModel tableModel;
    private JTextField nameField, hostField, portField, usernameField, passwordField, databaseField;
    private JButton saveButton, validateButton, cancelButton;
    private ConnectionManager connectionManager;
    private String editedConnectionName = null;

    public ManageConnectionsDialog(ConnectionManager connectionManager, Frame parent) {

        super(parent, "ArangoDB Connection Manager", true);
        this.connectionManager = connectionManager;
        setLayout(new BorderLayout());
        setModalityType(ModalityType.APPLICATION_MODAL);
        setLocationRelativeTo(parent);

        UIUtils.setIconImage(this, "arangodb_icon.png");

        // Left Panel
        JPanel leftPanel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[] { "Name", "Host", "Port", "Edit", "Delete", "Validate" }, 0);
        connectionTable = new JTable(tableModel);
        leftPanel.add(new JScrollPane(connectionTable), BorderLayout.CENTER);

        // Right Panel
        JPanel rightPanel = createRightPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // Load existing connections
        loadConnections();

        // Event Handling
        saveButton.addActionListener(e -> saveConnection());
        validateButton.addActionListener(e -> validateConnection(false));
        cancelButton.addActionListener(e -> clearFields());

        connectionTable.getColumn("Edit").setCellRenderer(new ButtonRenderer("Edit"));
        connectionTable.getColumn("Edit")
                .setCellEditor(new ButtonEditor(new JCheckBox(), "Edit", e -> editConnection()));

        connectionTable.getColumn("Delete").setCellRenderer(new ButtonRenderer("Delete"));
        connectionTable.getColumn("Delete")
                .setCellEditor(new ButtonEditor(new JCheckBox(), "Delete", e -> deleteConnection()));

        connectionTable.getColumn("Validate").setCellRenderer(new ButtonRenderer("Validate"));
        connectionTable.getColumn("Validate")
                .setCellEditor(new ButtonEditor(new JCheckBox(), "Validate", e -> validateConnection(true)));

        setSize(1200, 600);
        setLocationRelativeTo(parent);
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        rightPanel.add(new JLabel("Name:"), gbc);
        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(200, 25));
        gbc.gridx = 1;
        rightPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        rightPanel.add(new JLabel("Host:"), gbc);
        hostField = new JTextField();
        hostField.setPreferredSize(new Dimension(200, 25));
        gbc.gridx = 1;
        rightPanel.add(hostField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        rightPanel.add(new JLabel("Port:"), gbc);
        portField = new JTextField();
        portField.setPreferredSize(new Dimension(200, 25));
        ((AbstractDocument) portField.getDocument()).setDocumentFilter(new NumericDocumentFilter());
        gbc.gridx = 1;
        rightPanel.add(portField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        rightPanel.add(new JLabel("Username:"), gbc);
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(200, 25));
        gbc.gridx = 1;
        rightPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        rightPanel.add(new JLabel("Password:"), gbc);
        passwordField = new JTextField();
        passwordField.setPreferredSize(new Dimension(200, 25));
        gbc.gridx = 1;
        rightPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        rightPanel.add(new JLabel("Database:"), gbc);
        databaseField = new JTextField();
        databaseField.setPreferredSize(new Dimension(200, 25));
        gbc.gridx = 1;
        rightPanel.add(databaseField, gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel();
        saveButton = new JButton("Save");
        validateButton = new JButton("Validate");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(validateButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        rightPanel.add(buttonPanel, gbc);

        return rightPanel;
    }

    private void loadConnections() {
        Map<String, ConnectionDetails> connections = connectionManager.getAllConnections();
        for (Map.Entry<String, ConnectionDetails> entry : connections.entrySet()) {
            ConnectionDetails details = entry.getValue();
            tableModel.addRow(new Object[] { entry.getKey(), details.getHost(), details.getPort(), "Edit", "Delete",
                    "Validate" });
        }
    }

    private void saveConnection() {
        String name = nameField.getText();
        String host = hostField.getText();
        int port = Integer.parseInt(portField.getText());
        String username = usernameField.getText();
        String password = passwordField.getText();
        String database = databaseField.getText();

        ConnectionDetails connectionDetails = new ConnectionDetails(host, port, username, password, database);

        if (editedConnectionName != null && !editedConnectionName.equals(name)) {
            connectionManager.removeConnection(editedConnectionName);
        }

        connectionManager.addConnection(name, connectionDetails);

        // Update the table model
        boolean found = false;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).equals(editedConnectionName)) {
                tableModel.setValueAt(name, i, 0);
                tableModel.setValueAt(host, i, 1);
                tableModel.setValueAt(port, i, 2);
                found = true;
                break;
            }
        }

        if (!found) {
            tableModel.addRow(new Object[] { name, host, port, "Edit", "Delete", "Validate" });
        }

        clearFields();
        editedConnectionName = null;
    }

    private void clearFields() {
        nameField.setText("");
        hostField.setText("");
        portField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        databaseField.setText("");
        editedConnectionName = null;
    }

    private void editConnection() {
        int row = connectionTable.getSelectedRow();
        String name = (String) tableModel.getValueAt(row, 0);
        ConnectionDetails details = connectionManager.getConnection(name);
        if (details != null) {
            nameField.setText(name);
            hostField.setText(details.getHost());
            portField.setText(String.valueOf(details.getPort()));
            usernameField.setText(details.getUser());
            passwordField.setText(details.getPassword());
            databaseField.setText(details.getDatabase());
            editedConnectionName = name;
        }
    }

    private void deleteConnection() {
        int row = connectionTable.getSelectedRow();
        String name = (String) tableModel.getValueAt(row, 0);
        connectionManager.removeConnection(name);
        tableModel.removeRow(row);
    }

    private void validateConnection(boolean fromTable) {
        String message;
        try {
            boolean isValid;
            if (fromTable) {
                int row = connectionTable.getSelectedRow();
                String name = (String) tableModel.getValueAt(row, 0);
                isValid = connectionManager.validate(name);
            } else {
                String name = nameField.getText();
                String host = hostField.getText();
                int port = Integer.parseInt(portField.getText());
                String username = usernameField.getText();
                String password = passwordField.getText();
                String database = databaseField.getText();

                ConnectionDetails connectionDetails = new ConnectionDetails(host, port, username, password, database);

                isValid = connectionManager.validate(connectionDetails);

            }
            message = isValid ? "Connection is valid." : "Connection is invalid.";
        } catch (Exception e) {
            LOGGER.warn("Connection is invalid", e);
            message = String.format("Connection is invalid: %s", e.getMessage());
        }
        showValidationMessage(message);
    }

    private void showValidationMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    // Custom DocumentFilter to allow only numeric input
    class NumericDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string.matches("\\d*")) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text.matches("\\d*")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            super.remove(fb, offset, length);
        }
    }
}