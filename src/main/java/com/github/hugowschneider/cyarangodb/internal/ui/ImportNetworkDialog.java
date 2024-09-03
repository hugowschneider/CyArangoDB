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

public class ImportNetworkDialog extends BaseNetworkDialog {

    private NetworkManager networkManager;
    private JTextField networkNameField;

    public ImportNetworkDialog(ConnectionManager connectionManager, NetworkManager networkManager, JFrame parentFrame) {
        super(connectionManager, parentFrame, "Import Network");
        this.networkManager = networkManager;

        this.networkNameField.setText(suggestNetworkName());
    }

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

    @Override
    protected void processQueryResult(List<RawJson> docs, ArangoDatabase database, String query)
            throws ImportNetworkException {

        NetworkImportResult result = networkManager.importNetwork(docs, database, networkNameField.getText().trim(),
                query);
        JOptionPane.showMessageDialog(this,
                String.format("Network imported with %1$d nodes and %2$d edges", result.getNodeCount(),
                        result.getEdgeCount()));

    }

    @Override
    protected Component renderTopComponent() {
        JLabel networkNameLabel = new JLabel("Network Name:");
        JPanel networkNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        this.networkNameField = new JTextField(20);
        networkNamePanel.add(networkNameLabel);
        networkNamePanel.add(networkNameField);

        return networkNamePanel;
    }

    @Override
    protected Component renderCenterComponent() {
        return null;
    }
}