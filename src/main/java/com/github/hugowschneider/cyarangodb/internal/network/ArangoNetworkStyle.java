package com.github.hugowschneider.cyarangodb.internal.network;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyRow;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArangoNetworkStyle {

    private static final Color[] COLOR_TABLE = {
            new Color(0, 128, 128), // Teal
            new Color(0, 255, 255), // Cyan
            new Color(0, 100, 0), // Dark Green
            new Color(0, 255, 127), // Spring Green
            new Color(0, 206, 209), // Dark Turquoise
            new Color(64, 224, 208), // Turquoise
            new Color(32, 178, 170), // Light Sea Green
            new Color(72, 209, 204), // Medium Turquoise
            new Color(0, 139, 139), // Dark Cyan
            new Color(0, 128, 0), // Green
            new Color(46, 139, 87), // Sea Green
            new Color(60, 179, 113), // Medium Sea Green
            new Color(102, 205, 170), // Medium Aquamarine
            new Color(127, 255, 212), // Aquamarine
            new Color(0, 250, 154), // Medium Spring Green
            new Color(144, 238, 144) // Light Green
    };

    private static Map<String, Color> collectionColorMap = new HashMap<>();

    public static void applyStyles(CyNetworkViewManager networkViewManager) {
        for (CyNetworkView networkView : networkViewManager.getNetworkViewSet()) {
            applyNodeStyles(networkView);
            applyEdgeStyles(networkView);
            networkView.updateView();
        }
    }

    private static void applyNodeStyles(CyNetworkView networkView) {
        List<CyNode> nodes = networkView.getModel().getNodeList();
        for (CyNode node : nodes) {
            CyRow row = networkView.getModel().getRow(node);
            String collectionName = row.get("Collection", String.class);
            Color color = getColorForCollection(collectionName);
            String label = getLabelForNode(row);

            networkView.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, color);
            networkView.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_LABEL, label);
        }
    }

    private static void applyEdgeStyles(CyNetworkView networkView) {
        List<CyEdge> edges = networkView.getModel().getEdgeList();
        for (CyEdge edge : edges) {
            CyRow row = networkView.getModel().getRow(edge);
            String label = getLabelForEdge(row);

            networkView.getEdgeView(edge).setVisualProperty(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT,
                    Color.DARK_GRAY);
            networkView.getEdgeView(edge).setVisualProperty(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE,
                    ArrowShapeVisualProperty.ARROW);
            networkView.getEdgeView(edge).setVisualProperty(BasicVisualLexicon.EDGE_LABEL, label);
        }
    }

    private static Color getColorForCollection(String collectionName) {
        return collectionColorMap.computeIfAbsent(collectionName,
                k -> COLOR_TABLE[Math.abs(collectionName.hashCode()) % COLOR_TABLE.length]);
    }

    private static String getLabelForNode(CyRow row) {
        return row.get("name", String.class);
    }

    private static String getLabelForEdge(CyRow row) {
        return row.get("Collection", String.class);
    }
}