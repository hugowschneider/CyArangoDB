package com.github.hugowschneider.cyarangodb.internal.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.github.hugowschneider.cyarangodb.internal.Constants;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;

public class DetailDialog extends JDialog {
    private JLabel idLabel;
    private JLabel keyLabel;
    private JLabel collectionLabel;
    private JLabel fromLabel;
    private JLabel toLabel;
    private RSyntaxTextArea jsonTextArea;
    private JButton closeButton;

    public DetailDialog(ConnectionManager connectionManager, JFrame parent,
            CyEdge edge, CyNetwork network) {
        this(connectionManager, parent, network, true);

        CyRow row = network.getDefaultEdgeTable().getRow(edge.getSUID());
        idLabel.setText(row.get(Constants.EdgeColumns.ID, String.class));
        keyLabel.setText(row.get(Constants.EdgeColumns.KEY, String.class));
        collectionLabel.setText(row.get(Constants.EdgeColumns.COLLECTION, String.class));
        fromLabel.setText(row.get(Constants.EdgeColumns.FROM, String.class));
        toLabel.setText(row.get(Constants.EdgeColumns.TO, String.class));
        jsonTextArea.setText(row.get(Constants.EdgeColumns.DATA, String.class));
    }

    public DetailDialog(ConnectionManager connectionManager, JFrame parent,
            CyNode node, CyNetwork network) {
        this(connectionManager, parent, network, false);
        CyRow row = network.getDefaultNodeTable().getRow(node.getSUID());
        idLabel.setText(row.get(Constants.NodeColumns.ID, String.class));
        keyLabel.setText(row.get(Constants.NodeColumns.KEY, String.class));
        collectionLabel.setText(row.get(Constants.NodeColumns.COLLECTION, String.class));
        jsonTextArea.setText(row.get(Constants.NodeColumns.DATA, String.class));
    }

    private DetailDialog(ConnectionManager connectionManager, JFrame parent,
            CyNetwork network, boolean isEdge) {
        super(parent, "Detail Dialog", true);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new GridLayout(0, 1));

        idLabel = new JLabel();
        keyLabel = new JLabel();
        collectionLabel = new JLabel();

        headerPanel.add(createLabelPanel("ID: ", "", idLabel));
        headerPanel.add(createLabelPanel("Key: ", "", keyLabel));
        headerPanel.add(createLabelPanel("Collection: ", "", collectionLabel));

        if (isEdge) {
            fromLabel = new JLabel();
            toLabel = new JLabel();
            headerPanel.add(createLabelPanel("From: ", "", fromLabel));
            headerPanel.add(createLabelPanel("To: ", "", toLabel));
        }

        add(headerPanel, BorderLayout.NORTH);

        // JSON Text Area
        jsonTextArea = new RSyntaxTextArea(20, 60);
        jsonTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        jsonTextArea.setCodeFoldingEnabled(true);
        jsonTextArea.setText("{ }");
        jsonTextArea.setEditable(false);
        RTextScrollPane sp = new RTextScrollPane(jsonTextArea);
        add(sp, BorderLayout.CENTER);

        // Close Button
        closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        // Refresh Button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Define refresh behavior here
                refresh();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private void refresh() {
        // Define refresh behavior here
    }

    private JPanel createLabelPanel(String labelText, String valueText, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(labelText);

        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD));

        // Add margin to the panel
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Add margin to the labels
        label.setBorder(new EmptyBorder(5, 5, 5, 5));
        valueLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        panel.add(label, BorderLayout.WEST);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }
}