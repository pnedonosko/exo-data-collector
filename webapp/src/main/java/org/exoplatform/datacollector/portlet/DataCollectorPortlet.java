package org.exoplatform.datacollector.portlet;

import javax.portlet.GenericPortlet;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.application.WebuiRequestContext;

public class DataCollectorPortlet extends GenericPortlet {

	/**
	 * Renders the portlet on the page.
	 */
	@Override
	protected void doView(final RenderRequest request, final RenderResponse response) {
		JavascriptManager js = ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).getJavascriptManager();
		js.require("SHARED/datacollector", "datacollector");
	}

}
