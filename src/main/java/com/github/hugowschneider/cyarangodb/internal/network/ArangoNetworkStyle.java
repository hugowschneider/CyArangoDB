package com.github.hugowschneider.cyarangodb.internal.network;

import java.awt.Color;
import java.awt.Paint;
import java.util.Iterator;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;

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

    private VisualMappingManager visualMappingManager;
    private VisualStyleFactory visualStyleFactory;    
    private VisualMappingFunctionFactory mappingFunctionFactoryContinues;
    private VisualMappingFunctionFactory mappingFunctionFactoryDiscrete;
    private VisualMappingFunctionFactory mappingFunctionPassthorugh;

    private final String STYLE_NAME = "ArangoDB Style";

    public ArangoNetworkStyle(VisualMappingManager visualMappingManager,
            VisualStyleFactory visualStyleFactory,
            VisualMappingFunctionFactory mappingFunctionFactoryContinues,
            VisualMappingFunctionFactory mappingFunctionFactoryDiscrete,
            VisualMappingFunctionFactory mappingFunctionPassthorugh) {
        this.mappingFunctionFactoryContinues = mappingFunctionFactoryContinues;
        this.mappingFunctionFactoryDiscrete = mappingFunctionFactoryDiscrete;
        this.mappingFunctionPassthorugh = mappingFunctionPassthorugh;
        this.visualMappingManager = visualMappingManager;
        this.visualStyleFactory = visualStyleFactory;

        createStyle();

    }

    public void createStyle() {
        Iterator<VisualStyle> it = visualMappingManager.getAllVisualStyles().iterator();
        while (it.hasNext()) {
            VisualStyle visualStyle = (VisualStyle) it.next();
            if (visualStyle.getTitle().equalsIgnoreCase(STYLE_NAME)) {
                visualMappingManager.removeVisualStyle(visualStyle);
                break;
            }
        }
        VisualStyle arangoDBStyle = visualStyleFactory.createVisualStyle(STYLE_NAME);
        arangoDBStyle.setTitle(STYLE_NAME);

        // Set node styles
        DiscreteMapping<String, NodeShape> nodeShapeMapping = (DiscreteMapping<String, NodeShape>) mappingFunctionFactoryDiscrete
                .createVisualMappingFunction("Collection", String.class, BasicVisualLexicon.NODE_SHAPE);
        nodeShapeMapping.putMapValue("value1", NodeShapeVisualProperty.ELLIPSE);
        // Add more mappings for different collection values if needed

        DiscreteMapping<Integer, Paint> nodeColorMapping = (DiscreteMapping<Integer, Paint>) mappingFunctionFactoryDiscrete
                .createVisualMappingFunction("Color", Integer.class, BasicVisualLexicon.NODE_FILL_COLOR);
        for (int i = 0; i < ArangoNetworkStyle.COLOR_TABLE.length; i++) {
            nodeColorMapping.putMapValue(i, ArangoNetworkStyle.COLOR_TABLE[i]);
        }
        // Add more mappings for different collection values if needed

        VisualMappingFunction<String, String> nodeLabelMapping = mappingFunctionPassthorugh
                .createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_LABEL);

        arangoDBStyle.addVisualMappingFunction(nodeShapeMapping);
        arangoDBStyle.addVisualMappingFunction(nodeColorMapping);
        arangoDBStyle.addVisualMappingFunction(nodeLabelMapping);

        // Set edge styles
        DiscreteMapping<String, ArrowShape> edgeArrowMapping = (DiscreteMapping<String, ArrowShape>) mappingFunctionFactoryDiscrete
                .createVisualMappingFunction("Collection", String.class, BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE);
        edgeArrowMapping.putMapValue("value1", ArrowShapeVisualProperty.ARROW);
        // Add more mappings for different collection values if needed

        DiscreteMapping<String, Paint> edgeColorMapping = (DiscreteMapping<String, Paint>) mappingFunctionFactoryDiscrete
                .createVisualMappingFunction("Collection", String.class,
                        BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
        edgeColorMapping.putMapValue("value1", Color.DARK_GRAY);

        // Add more mappings for different collection values if needed

        VisualMappingFunction<String, String> edgeLabelMapping = mappingFunctionPassthorugh
                .createVisualMappingFunction("Collection", String.class, BasicVisualLexicon.EDGE_LABEL);

        arangoDBStyle.addVisualMappingFunction(edgeArrowMapping);
        arangoDBStyle.addVisualMappingFunction(edgeColorMapping);
        arangoDBStyle.addVisualMappingFunction(edgeLabelMapping);

        visualMappingManager.addVisualStyle(arangoDBStyle);

    }

    public void applyStyles(CyNetworkViewManager networkViewManager) {
        Iterator<VisualStyle> it = visualMappingManager.getAllVisualStyles().iterator();

        while (it.hasNext()) {
            VisualStyle visualStyle = it.next();
            if (visualStyle.getTitle().equalsIgnoreCase(STYLE_NAME)) {
                for (CyNetworkView networkView : networkViewManager.getNetworkViewSet()) {
                    visualMappingManager.setVisualStyle(visualStyle, networkView);
                }
                break;
            }
        }

    }

    public static int computeColorIndex(String collectionName) {
        return Math.abs(collectionName.hashCode() % COLOR_TABLE.length);
    }

}