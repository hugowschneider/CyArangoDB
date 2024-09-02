package com.github.hugowschneider.cyarangodb.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.osgi.framework.BundleContext;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.ui.ImportNetworkAction;
import com.github.hugowschneider.cyarangodb.internal.ui.ManageConnectionsAction;
import java.util.Properties;
import javax.swing.JFrame;

public class CyActivator extends AbstractCyActivator {

	@Override
	public void start(BundleContext context) throws Exception {

		CySwingApplication cySwingApplication = getService(context, CySwingApplication.class);

		CyNetworkFactory networkFactory = getService(context, CyNetworkFactory.class);
		CyNetworkManager networkManager = getService(context, CyNetworkManager.class);
		CyNetworkViewManager networkViewManager = getService(context, CyNetworkViewManager.class);
		CyNetworkViewFactory networkViewFactory = getService(context, CyNetworkViewFactory.class);
		CyApplicationManager applicationManager = getService(context, CyApplicationManager.class);

		ConnectionManager connectionManager = new ConnectionManager(networkFactory, networkManager, networkViewFactory,
				networkViewManager, applicationManager);

		JFrame cytoscapeMain = cySwingApplication.getJFrame();

		ManageConnectionsAction manageConnectionsAction = new ManageConnectionsAction(connectionManager, cytoscapeMain);
		ImportNetworkAction importNetworkAction = new ImportNetworkAction(connectionManager, cytoscapeMain);

		registerAllServices(context, manageConnectionsAction, new Properties());
		registerAllServices(context, importNetworkAction, new Properties());

	}
}