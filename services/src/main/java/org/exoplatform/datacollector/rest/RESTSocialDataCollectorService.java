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

import java.util.concurrent.Callable;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.exoplatform.datacollector.SocialDataCollectorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

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

  /** Instantiates a new REST service for the DataCollector */
  public RESTSocialDataCollectorService(SocialDataCollectorService dataCollectorService) {
    this.dataCollector = dataCollectorService;
  }

  /**
   * Start the collector.
   *
   * @return response 200 which contains relevanceEntity or 404
   */
  @GET
  // @RolesAllowed("administrators") // TODO only super users in PROD mode
  @RolesAllowed("users")
  @Path("/run/{bucketname}")
  public Response runCollect(@PathParam("bucketname") String bucketName) {
    try {
      // TODO do asynchronous
      /*getExecutorService().submit(new Callable<Void>() {
          @Override
          public Void call() throws Exception {*/
      String bucketPath = dataCollector.collectUsersActivities(bucketName);
      String actualBucketName = bucketPath.substring(bucketPath.lastIndexOf('/'));
      return Response.ok().entity("{\"bucketname\":\"" + actualBucketName + "\"}").build();
    } catch (Exception e) {
      LOG.error("Error collecting user activities into " + bucketName, e);
      return Response.serverError().entity("{\"error\":\"Error collecting user activities\"}").build();
    }

  }

}
