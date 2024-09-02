package com.github.hugowschneider.cyarangodb.internal;

import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.NODE_APPS_MENU;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.network.NetworkManager;
import com.github.hugowschneider.cyarangodb.internal.task.ExpandNodeContextMenuFactory;
import com.github.hugowschneider.cyarangodb.internal.ui.ImportNetworkAction;
import com.github.hugowschneider.cyarangodb.internal.ui.ManageConnectionsAction;
import java.util.Properties;
import javax.swing.JFrame;

public class CyActivator extends AbstractCyActivator {

	@Override
	public void start(BundleContext context) throws Exception {

		CySwingApplication cySwingApplication = getService(context, CySwingApplication.class);

		CyNetworkFactory cyNetworkFactory = getService(context, CyNetworkFactory.class);
		CyNetworkManager cyNetworkManager = getService(context, CyNetworkManager.class);
		CyNetworkViewManager cyNetworkViewManager = getService(context, CyNetworkViewManager.class);
		CyNetworkViewFactory cyNetworkViewFactory = getService(context, CyNetworkViewFactory.class);
		CyApplicationManager cyApplicationManager = getService(context, CyApplicationManager.class);
		CyServiceRegistrar cyServiceRegistrar = getService(context, CyServiceRegistrar.class);
		CyLayoutAlgorithmManager cyLayoutAlgorithmManager = getService(context, CyLayoutAlgorithmManager.class);
		TaskManager<?, ?> taskManager = getService(context, TaskManager.class);

		ConnectionManager connectionManager = new ConnectionManager();
		NetworkManager networkManager = new NetworkManager(cyNetworkFactory, cyNetworkManager, cyNetworkViewFactory,
				cyNetworkViewManager, cyApplicationManager, cyLayoutAlgorithmManager, taskManager);

		// Manu actions

		JFrame cytoscapeMain = cySwingApplication.getJFrame();
		ManageConnectionsAction manageConnectionsAction = new ManageConnectionsAction(connectionManager,
				cytoscapeMain);
		ImportNetworkAction importNetworkAction = new ImportNetworkAction(connectionManager, networkManager,
				cytoscapeMain);

		registerAllServices(context, manageConnectionsAction, new Properties());
		registerAllServices(context, importNetworkAction, new Properties());

		// Node Context Manu actions

		ExpandNodeContextMenuFactory factory = new ExpandNodeContextMenuFactory(cyServiceRegistrar, networkManager,
				connectionManager,
				cytoscapeMain);

		Properties props = new Properties();
		props.setProperty("preferredTaskManager", "menu");
		props.setProperty(PREFERRED_MENU, NODE_APPS_MENU);
		props.setProperty(MENU_GRAVITY, "10.0");
		props.setProperty(TITLE, "Extend Network using ArangoDB Query");
		registerService(context, factory, NodeViewTaskFactory.class, props);

	}
}