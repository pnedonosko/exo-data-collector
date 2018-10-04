package org.exoplatform.datacollector.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.exoplatform.services.rest.resource.ResourceContainer;

@Path("/datacollector")
@Produces("application/json")
public class RESTDataCollectorService implements ResourceContainer {

	@GET
	@Path("/hello/{name}")
	public String hello(@PathParam("name") String name) {
		return "Hello " + name;
	}

}
