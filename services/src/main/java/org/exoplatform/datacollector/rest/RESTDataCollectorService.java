package org.exoplatform.datacollector.rest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.exoplatform.datacollector.DataCollectorService;
import org.exoplatform.datacollector.domain.RelevanceEntity;
import org.exoplatform.datacollector.domain.RelevanceId;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.json.JSONException;

@Path("/datacollector")
@Produces(MediaType.APPLICATION_JSON)
public class RESTDataCollectorService implements ResourceContainer {

	/** The Constant LOG. */
	protected static final Log LOG = ExoLogger.getLogger(RESTDataCollectorService.class);

	protected final DataCollectorService dataCollectorService;

	public RESTDataCollectorService(DataCollectorService dataCollectorService) {
		this.dataCollectorService = dataCollectorService;
	}

	@POST
	@RolesAllowed("users")
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/collector")
	public void saveRelevance(RelevanceEntity relevanceEntity) throws JSONException {
		dataCollectorService.saveRelevance(relevanceEntity);
	}
	
	@GET
	@Path("/collector/{userId}/{activityId}")
	public Response getRelevance(@PathParam("userId") String userId, @PathParam("activityId") String activityId) {
		RelevanceEntity relevanceEntity = dataCollectorService.findById(new RelevanceId(userId, activityId));
		if(relevanceEntity == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		return Response.ok().entity(relevanceEntity).build();
	}

	
}
