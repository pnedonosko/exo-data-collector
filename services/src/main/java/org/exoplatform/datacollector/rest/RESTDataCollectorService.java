package org.exoplatform.datacollector.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.exoplatform.datacollector.domain.RelevanceEntity;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.json.JSONException;

@Path("/datacollector")
@Produces(MediaType.APPLICATION_JSON)
public class RESTDataCollectorService implements ResourceContainer {

	/** The Constant LOG. */
	protected static final Log LOG = ExoLogger.getLogger(RESTDataCollectorService.class);
	
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/collector")
	public void collect(RelevanceEntity relevanceEntity) throws JSONException {
		LOG.info("New Relevance: " + relevanceEntity.toString());
	}

}
