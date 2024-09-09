package com.github.hugowschneider.cyarangodb.internal.network;

import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.values.VisualPropertyValue;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ArangoNetworkStyleTest {

    private static final String STYLE_NAME = "ArangoDB Style";

    @Mock
    private VisualMappingManager visualMappingManager;
    @Mock
    private VisualStyleFactory visualStyleFactory;
    @Mock
    private VisualMappingFunctionFactory mappingFunctionFactoryDiscrete;
    @Mock
    private VisualMappingFunctionFactory mappingFunctionPassthrough;
    @Mock
    private VisualStyle mockVisualStyle;

    private ArangoNetworkStyle arangoNetworkStyle;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Stub visualStyleFactory.createVisualStyle to return the mock style
        when(mappingFunctionFactoryDiscrete.createVisualMappingFunction(anyString(), any(Class.class),
                or(any(VisualProperty.class), isNull(VisualProperty.class))))
                .thenReturn(mock(DiscreteMapping.class));
        when(visualStyleFactory.createVisualStyle(anyString())).thenReturn(mockVisualStyle);
        // Mock visual styles to return an empty set initially
        when(visualMappingManager.getAllVisualStyles()).thenReturn(Collections.emptySet());

        arangoNetworkStyle = new ArangoNetworkStyle(visualMappingManager, visualStyleFactory,
                mappingFunctionFactoryDiscrete, mappingFunctionPassthrough);
    }

    @Test
    void testCreateStyle() {
        // Verify that a new VisualStyle was created
        verify(visualStyleFactory).createVisualStyle(STYLE_NAME);
        // Verify that the visual style was added to the manager
        verify(visualMappingManager).addVisualStyle(mockVisualStyle);

        // Verify that the mappings were added to the visual style
        verify(mockVisualStyle, atLeastOnce()).addVisualMappingFunction(any());
    }

    @Test
    void testApplyStyles() {
        CyNetworkViewManager mockNetworkViewManager = mock(CyNetworkViewManager.class);
        CyNetworkView mockNetworkView = mock(CyNetworkView.class);

        // Setup a set with a single network view
        when(mockNetworkViewManager.getNetworkViewSet()).thenReturn(Collections.singleton(mockNetworkView));
        when(visualMappingManager.getAllVisualStyles()).thenReturn(Collections.singleton(mockVisualStyle));
        when(mockVisualStyle.getTitle()).thenReturn(STYLE_NAME);

        // Call the method
        arangoNetworkStyle.applyStyles(mockNetworkViewManager);

        // Verify that the correct visual style was applied to the network view
        verify(visualMappingManager).setVisualStyle(mockVisualStyle, mockNetworkView);
    }

    @Test
    void testComputeColorIndex() {
        // Simple test for computeColorIndex logic
        int index = ArangoNetworkStyle.computeColorIndex("TestCollection");
        assert (index >= 0 && index < ArangoNetworkStyle.COLOR_TABLE.length);
    }
}
