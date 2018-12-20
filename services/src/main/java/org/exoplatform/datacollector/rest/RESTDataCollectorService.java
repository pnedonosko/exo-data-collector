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

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.datacollector.SocialDataCollectorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * The REST service for Social Data Collectors
 */
@Path("/datacollector")
@Produces(MediaType.APPLICATION_JSON)
public class RESTDataCollectorService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log                 LOG = ExoLogger.getLogger(RESTDataCollectorService.class);

  /** The Data Collector service */
  protected final SocialDataCollectorService dataCollector;

  /** Instantiates a new REST service for the DataCollector */
  public RESTDataCollectorService(SocialDataCollectorService dataCollectorService) {
    this.dataCollector = dataCollectorService;
  }

  /**
   * Start the collector.
   *
   * @return response 200 which contains relevanceEntity or 404
   */
  @GET
  @RolesAllowed("administrators")
  @Path("/start")
  public Response start() {
    // TODO
    return Response.ok().build();
  }

}
