package com.github.hugowschneider.cyarangodb.internal;

import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.NODE_APPS_MENU;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import javax.swing.JFrame;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.task.EdgeViewTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.network.ArangoNetworkStyle;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkManager;
import com.github.hugowschneider.cyarangodb.internal.ui.ImportNetworkAction;
import com.github.hugowschneider.cyarangodb.internal.ui.ManageConnectionsAction;
import com.github.hugowschneider.cyarangodb.internal.ui.task.EdgeDetailContextMenuFactory;
import com.github.hugowschneider.cyarangodb.internal.ui.task.ExpandNetworkContextMenuFactory;
import com.github.hugowschneider.cyarangodb.internal.ui.task.ExpandNodeContextMenuFactory;
import com.github.hugowschneider.cyarangodb.internal.ui.task.NodeDetailContextMenuFactory;

/**
 * The activator for the CyArangoDB application.
 */
public class CyActivator extends AbstractCyActivator {

	/**
	 * The logger for the activator.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(CyActivator.class);

	/**
	 * Creates a new CyActivator.
	 */
	public CyActivator() {
		super();
	}

	/**
	 * Starts the CyArangoDB application.
	 *
	 * @param context the bundle context
	 * @throws Exception if an error occurs while starting the application
	 */
	@Override
	public void start(BundleContext context) throws Exception {

		LOGGER.debug("Starting CyArangoDB Application...");

		CySwingApplication cySwingApplication = getService(context, CySwingApplication.class);

		CyNetworkFactory cyNetworkFactory = getService(context, CyNetworkFactory.class);
		CyNetworkManager cyNetworkManager = getService(context, CyNetworkManager.class);
		CyNetworkViewManager cyNetworkViewManager = getService(context, CyNetworkViewManager.class);
		CyNetworkViewFactory cyNetworkViewFactory = getService(context, CyNetworkViewFactory.class);
		CyApplicationManager cyApplicationManager = getService(context, CyApplicationManager.class);
		CyLayoutAlgorithmManager cyLayoutAlgorithmManager = getService(context, CyLayoutAlgorithmManager.class);
		TaskManager<?, ?> taskManager = getService(context, TaskManager.class);

		VisualMappingManager visualMappingManager = getService(context, VisualMappingManager.class);

		VisualStyleFactory visualStyleFactory = getService(context, VisualStyleFactory.class);

		VisualMappingFunctionFactory mappingFunctionFactoryDiscrete = getService(context,
				VisualMappingFunctionFactory.class,
				"(mapping.type=discrete)");
		VisualMappingFunctionFactory mappingFunctionPassthorugh = getService(context,
				VisualMappingFunctionFactory.class,
				"(mapping.type=passthrough)");

		ConnectionManager connectionManager = new ConnectionManager();
		ArangoNetworkStyle arangoNetworkStyle = new ArangoNetworkStyle(visualMappingManager, visualStyleFactory,
				mappingFunctionFactoryDiscrete, mappingFunctionPassthorugh);

		NetworkManager networkManager = new NetworkManager(cyNetworkFactory, cyNetworkManager, cyNetworkViewFactory,
				cyNetworkViewManager, cyApplicationManager, cyLayoutAlgorithmManager, taskManager, arangoNetworkStyle);

		// Manu actions

		JFrame cytoscapeMain = cySwingApplication.getJFrame();
		ManageConnectionsAction manageConnectionsAction = new ManageConnectionsAction(connectionManager,
				cytoscapeMain);
		ImportNetworkAction importNetworkAction = new ImportNetworkAction(connectionManager, networkManager,
				cytoscapeMain);

		registerAllServices(context, manageConnectionsAction, new Properties());
		registerAllServices(context, importNetworkAction, new Properties());

		// Context Manu actions
		{
			ExpandNodeContextMenuFactory factory = new ExpandNodeContextMenuFactory(networkManager,
					connectionManager,
					cytoscapeMain);

			Properties props = new Properties();
			props.setProperty("preferredTaskManager", "menu");
			props.setProperty(PREFERRED_MENU, NODE_APPS_MENU);
			props.setProperty(MENU_GRAVITY, "1.0");
			props.setProperty(TITLE, "Extend Network using ArangoDB Query");
			registerService(context, factory, NodeViewTaskFactory.class, props);
		}

		{
			ExpandNetworkContextMenuFactory factory = new ExpandNetworkContextMenuFactory(networkManager,
					connectionManager,
					cytoscapeMain);

			Properties props = new Properties();
			props.setProperty("preferredTaskManager", "menu");
			props.setProperty(PREFERRED_MENU, NODE_APPS_MENU);
			props.setProperty(MENU_GRAVITY, "1.0");
			props.setProperty(TITLE, "Extend Network using ArangoDB Query");
			registerService(context, factory, NetworkViewTaskFactory.class, props);
		}

		{
			EdgeDetailContextMenuFactory factory = new EdgeDetailContextMenuFactory(
					connectionManager,
					cytoscapeMain);

			Properties props = new Properties();
			props.setProperty("preferredTaskManager", "menu");
			props.setProperty(PREFERRED_MENU, NODE_APPS_MENU);
			props.setProperty(MENU_GRAVITY, "2.0");
			props.setProperty(TITLE, "Show Details");
			registerService(context, factory, EdgeViewTaskFactory.class, props);
		}

		{
			NodeDetailContextMenuFactory factory = new NodeDetailContextMenuFactory(
					connectionManager,
					cytoscapeMain);

			Properties props = new Properties();
			props.setProperty("preferredTaskManager", "menu");
			props.setProperty(PREFERRED_MENU, NODE_APPS_MENU);
			props.setProperty(MENU_GRAVITY, "2.0");
			props.setProperty(TITLE, "Show Details");
			registerService(context, factory, NodeViewTaskFactory.class, props);
		}
		LOGGER.debug("CyArangoDB Application started.");

	}
}