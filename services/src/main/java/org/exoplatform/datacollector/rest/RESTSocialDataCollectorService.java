/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.datacollector.rest;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.exoplatform.datacollector.SocialDataCollectorService;
import org.exoplatform.prediction.PredictionService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.ActivitiesRealtimeListAccess;
import org.exoplatform.social.core.identity.model.Identity;

/**
 * The REST service for Social Data Collectors
 */
@Path("/datacollector")
@Produces(MediaType.APPLICATION_JSON)
public class RESTSocialDataCollectorService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log                 LOG = ExoLogger.getLogger(RESTSocialDataCollectorService.class);

  /** The Data Collector service */
  protected final SocialDataCollectorService dataCollector;

  /** The predictor. */
  protected final PredictionService          predictor;

  /** Instantiates a new REST service for the DataCollector */
  public RESTSocialDataCollectorService(SocialDataCollectorService dataCollectorService, PredictionService predictor) {
    this.dataCollector = dataCollectorService;
    this.predictor = predictor;
  }

  /**
   * Starts collecting for all users of the platform.
   *
   * @return response 200 which contains status OK and bucketName, or status
   *         ERROR
   */
  @GET
  // @RolesAllowed("administrators") // TODO only super users in PROD mode
  @RolesAllowed("users")
  @Path("/run/{bucketname}")
  public Response runCollect(@PathParam("bucketname") String bucketName) {
    try {
      String bucketPath = dataCollector.collectUsersActivities(bucketName);
      String actualBucketName = bucketPath.substring(bucketPath.lastIndexOf(File.separator));
      return Response.ok().entity("{ \"status\": \"OK\", \"bucketName\": " + actualBucketName + " }").build();
    } catch (Exception e) {
      LOG.error("Error collecting user activities into " + bucketName, e);
      return Response.serverError().entity("{\"status\":\"Error collecting user activities\"}").build();
    }
  }

  /**
   * Start collect dataset and optionally train a model for a given user.
   *
   * @param bucketName the bucket name
   * @param userName the user name
   * @return response status ACCEPTED and userFolder
   */
  @GET
  // @RolesAllowed("administrators") // TODO only super users in PROD mode
  @RolesAllowed("users")
  @Path("/run/{bucketname}/{username}")
  public Response runCollect(@PathParam("bucketname") String bucketName,
                             @PathParam("username") String userName,
                             @QueryParam("train") String isTrain) {
    try {
      dataCollector.startUserCollector(userName, bucketName, isTrain != null ? Boolean.valueOf(isTrain) : false);
      return Response.ok().entity("{ \"status\": \"ACCEPTED\", \"userFoler\": " + bucketName + "/" + userName + "}").build();
    } catch (Exception e) {
      return Response.serverError().entity("{ \"status\": \"Error. " + e.getMessage() + "\"}").build();
    }
  }

  /**
   * Start the main loop with collecting and training
   *
   * @return response status OK or error
   */
  @GET
  // @RolesAllowed("administrators") // TODO only super users in PROD mode
  @RolesAllowed("users")
  @Path("/start")
  public Response run() {
    try {
      dataCollector.startMainLoop();
      return Response.ok().entity("{ \"status\": \"OK\"}").build();
    } catch (Exception e) {
      return Response.serverError().entity("{ \"status\": \"Error. " + e.getMessage() + "\"}").build();
    }
  }

  /**
   * Stops the main loop with collecting and training
   *
   * @return response status OK or error
   */
  @GET
  // @RolesAllowed("administrators") // TODO only super users in PROD mode
  @RolesAllowed("users")
  @Path("/stop")
  public Response stop() {
    try {
      dataCollector.stopMainLoop();
      return Response.ok().entity("{ \"status\": \"OK\"}").build();
    } catch (Exception e) {
      return Response.serverError().entity("{ \"status\": \"Error. " + e.getMessage() + "\"}").build();
    }
  }

  /**
   * Adds user for processing in the main loop.
   * 
   * @return response status OK or error
   */
  @GET
  // @RolesAllowed("administrators") // TODO only super users in PROD mode
  @RolesAllowed("users")
  @Path("/start/{username}")
  public Response addUser(@PathParam("username") String userName) {
    dataCollector.addUser(userName);
    return Response.ok().entity("{ \"status\": \"OK\"}").build();
  }

  @GET
  // @RolesAllowed("administrators") // TODO only super users in PROD mode
  @RolesAllowed("users")
  @Path("/feed/{username}")
  public Response predictUserFeedIds(@PathParam("username") String userName,
                                  @QueryParam("index") String sindex,
                                  @QueryParam("limit") String slimit) {
    // TODO it's not a right place for this service - move to
    // Training/Prediction REST
    if (userName != null && userName.length() > 0) {
      ConversationState convo = ConversationState.getCurrent();
      if (convo != null) {
        if (userName.equalsIgnoreCase("me")) {
          userName = convo.getIdentity().getUserId();
        }
        // XXX Right now, we let predict users only own feeds
        if (userName.equals(convo.getIdentity().getUserId())) {
          Identity userIdentity = dataCollector.getUserByName(userName);
          ActivitiesRealtimeListAccess predicted = predictor.getUserActivityFeed(userIdentity);
          int index = parseInt(sindex, 0);
          int limit = parseInt(sindex, 20);
          List<String> batch = predicted.loadIdsAsList(index, limit);
          return Response.ok()
                         .entity("{\"status\": \"OK\",\"activities\": \"" + batch.stream().collect(Collectors.joining(","))
                             + "\"}")
                         .build();
        } else {
          return Response.status(Status.FORBIDDEN).entity("{\"error\": \"\"User not match\"\"}").build();
        }
      } else {
        LOG.warn("ConversationState not set to get user feed");
        return Response.status(Status.UNAUTHORIZED).entity("{\"error\": \"\"User not authenticated\"\"}").build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity("{\"error\": \"\"User name required\"\"}").build();
    }
  }

  protected int parseInt(String intString, int defaultValue) {
    try {
      return Integer.parseInt(intString);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

}
