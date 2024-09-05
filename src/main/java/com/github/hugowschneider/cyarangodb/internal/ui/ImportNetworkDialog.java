package com.github.hugowschneider.cyarangodb.internal.ui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.arangodb.ArangoDatabase;
import com.arangodb.util.RawJson;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.network.ImportNetworkException;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkImportResult;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkManager;

/**
 * A dialog for importing a network from an ArangoDB database.
 */
public class ImportNetworkDialog extends BaseNetworkDialog {

    /**
     * The manager responsible for network operations.
     */
    private NetworkManager networkManager;

    /**
     * The text field for entering the network name.
     */
    private JTextField networkNameField;

    /**
     * Constructs a new ImportNetworkDialog.
     *
     * @param connectionManager the connection manager
     * @param networkManager    the network manager
     * @param parentFrame       the parent frame
     */
    public ImportNetworkDialog(ConnectionManager connectionManager, NetworkManager networkManager, JFrame parentFrame) {
        super(connectionManager, parentFrame, "Import Network");
        this.networkManager = networkManager;

        this.networkNameField.setText(suggestNetworkName());
    }

    /**
     * Suggests a unique network name based on existing network names.
     *
     * @return the suggested network name
     */
    private String suggestNetworkName() {
        List<String> existingNames = networkManager.getAllNetworkNames();
        String baseName = "ArangoNetwork";
        String suggestedName = baseName;
        int counter = 1;

        while (existingNames.contains(suggestedName)) {
            suggestedName = baseName + "_" + counter;
            counter++;
        }

        return suggestedName;
    }

    /**
     * Processes the query result and imports the network.
     *
     * @param docs     the list of RawJson documents
     * @param database the ArangoDatabase instance
     * @param query    the query string
     * @throws ImportNetworkException if an error occurs during network import
     */
    @Override
    protected void processQueryResult(List<RawJson> docs, ArangoDatabase database, String query)
            throws ImportNetworkException {

        NetworkImportResult result = networkManager.importNetwork(docs, database, networkNameField.getText().trim(),
                query);
        JOptionPane.showMessageDialog(this,
                String.format("Network imported with %1$d nodes and %2$d edges", result.getNodeCount(),
                        result.getEdgeCount()));

    }

    /**
     * Renders the top component of the dialog.
     *
     * @return the top component
     */
    @Override
    protected Component renderTopComponent() {
        JLabel networkNameLabel = new JLabel("Network Name:");
        JPanel networkNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        this.networkNameField = new JTextField(20);
        networkNamePanel.add(networkNameLabel);
        networkNamePanel.add(networkNameField);

        return networkNamePanel;
    }

    /**
     * Renders the center component of the dialog.
     *
     * @return the center component, or null if there is no center component
     */
    @Override
    protected Component renderCenterComponent() {
        return null;
    }
}