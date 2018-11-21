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
package org.exoplatform.datacollector;

import java.util.Collection;
import java.util.regex.Pattern;

import org.picocontainer.Startable;

import org.exoplatform.datacollector.dao.ActivityCommentedDAO;
import org.exoplatform.datacollector.dao.ActivityLikedDAO;
import org.exoplatform.datacollector.dao.ActivityMentionedDAO;
import org.exoplatform.datacollector.dao.ActivityPostedDAO;
import org.exoplatform.datacollector.domain.ActivityCommentedEntity;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.storage.api.IdentityStorage;

/**
 * The Class SocialDataCollectorService.
 */
public class SocialDataCollectorService implements Startable {

  /** Logger */
  private static final Log             LOG                 = ExoLogger.getExoLogger(SocialDataCollectorService.class);

  public static final int              BATCH_SIZE          = 200;

  protected static final Pattern       ENGINEERING_PATTERN =
                                                           Pattern.compile("^.*developer|architect|r&d|mobile|qa|fqa|tqa|test|quality|qualité|expert|integrator|designer|cwi|technical advisor|services delivery|software engineer.*$");

  protected static final Pattern       SALES_PATTERN       =
                                                     Pattern.compile("^.*consultant|sales|client|support|sales engineer|demand generator.*$");

  protected static final Pattern       MARKETING_PATTERN   =
                                                         Pattern.compile("^.*brand|communication|marketing|customer success|user experience|.*$");

  protected static final Pattern       MANAGEMENT_PATTERN  =
                                                          Pattern.compile("^.*officer|chief|founder|coo|cto|cio|evp|advisor|product manager|director|general manager.*$");

  protected static final Pattern       FINANCIAL_PATTERN   =
                                                         Pattern.compile("^.*accountant|financial|investment|account manager.*$");

  /** The Constant EMPLOYEE_GROUPID - TODO better make it configurable. */
  protected static final String        EMPLOYEE_GROUPID    = "/spaces/exo_employees";                                                                                                                                                   // for
                                                                                                                                                                                                                                        // /Groups/spaces/exo_employees

  protected final IdentityManager      identityManager;

  protected final IdentityStorage      identityStorage;

  protected final ActivityManager      activityManager;

  protected final OrganizationService  organization;

  protected final ActivityCommentedDAO commentStorage;
  
  protected final ActivityPostedDAO postStorage;
  
  protected final ActivityLikedDAO likeStorage;
  
  protected final ActivityMentionedDAO mentionStorage;

  /**
   * Instantiates a new data collector service.
   *
   * @param jcrService the jcr service
   * @param sessionProviders the session providers
   * @param hierarchyCreator the hierarchy creator
   * @param organization the organization
   * @param identityManager the identity manager
   * @param identityStorage the identity storage
   * @param activityManager the activity manager
   * @param commentStorage the comment storage
   */
  public SocialDataCollectorService(RepositoryService jcrService,
                                    SessionProviderService sessionProviders,
                                    NodeHierarchyCreator hierarchyCreator,
                                    OrganizationService organization,
                                    IdentityManager identityManager,
                                    IdentityStorage identityStorage,
                                    ActivityManager activityManager,
                                    ActivityPostedDAO postStorage,
                                    ActivityCommentedDAO commentStorage,
                                    ActivityLikedDAO likeStorage,
                                    ActivityMentionedDAO mentionStorage) {
    super();
    this.identityManager = identityManager;
    this.identityStorage = identityStorage;
    this.activityManager = activityManager;
    this.organization = organization;
    this.postStorage = postStorage;
    this.commentStorage = commentStorage;
    this.likeStorage = likeStorage;
    this.mentionStorage = mentionStorage;
    
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    // Nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    // Nothing
  }

  // **** internals

  /**
   * Collect user activities.
   *
   * @throws IllegalArgumentException the illegal argument exception
   * @throws Exception the exception
   */
  protected void collectUserActivities() throws IllegalArgumentException, Exception {
    ProfileFilter filter = new ProfileFilter();
    long idsCount = identityStorage.getIdentitiesByProfileFilterCount(OrganizationIdentityProvider.NAME, filter);
    long idsSteps = idsCount / BATCH_SIZE;
    int idsIndex = 0;
    for (int si = 0; si < idsSteps; si++) {
      // FYI flag for profile loading not used inside the call
      for (Identity id : identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME, filter, true)
                                        .load(idsIndex, BATCH_SIZE)) {
        // TODO find this user favorite participants (influencers) and spaces
        // TODO find this user favorite participants (influencers) and spaces
        UserInfluencers influencers = new UserInfluencers();
        influencers.addCommentedPoster(commentStorage.findPartIsCommentedPoster(id.getId()));
        influencers.addCommentedCommenter(commentStorage.findPartIsCommentedCommenter(id.getId()));
        influencers.addCommentedConvoPoster(commentStorage.findPartIsCommentedConvoPoster(id.getId()));
        
        influencers.addPostCommenter(commentStorage.findPartIsPostCommenter(id.getId()));
        influencers.addCommentCommenter(commentStorage.findPartIsCommentCommenter(id.getId()));
        influencers.addConvoCommenter(commentStorage.findPartIsConvoCommenter(id.getId()));
        
        influencers.addMentioner(mentionStorage.findPartIsMentioner(id.getId()));
        influencers.addMentioned(mentionStorage.findPartIsMentioned(id.getId()));
        
        influencers.addLikedPoster(likeStorage.findPartIsLikedPoster(id.getId()));
        // TODO
        
        //
        RealtimeListAccess<ExoSocialActivity> spacesActivities = activityManager.getActivitiesOfUserSpacesWithListAccess(id);
        // spacesActivities.loadNewer(sinceTime, limit);
        // spacesActivities.getNumberOfNewer(sinceTime);
        RealtimeListAccess<ExoSocialActivity> connsActivities = activityManager.getActivitiesOfConnectionsWithListAccess(id);
        // activityManager.getActivitiesWithListAccess(ownerIdentity,
        // viewerIdentity)

        // load identity's activities and collect its data
        RealtimeListAccess<ExoSocialActivity> feed = activityManager.getActivityFeedWithListAccess(id);
        int feedSize = feed.getSize();
        int feedSteps = feedSize / BATCH_SIZE;
        int feedIndex = 0;
        for (int fi = 0; fi < feedSteps; fi++) {
          for (ExoSocialActivity activity : feed.load(feedIndex, BATCH_SIZE)) {

          }
          feedIndex += BATCH_SIZE;
        }
      }
      idsIndex += BATCH_SIZE;
    }

  }

  protected StringBuilder activityHeader() {
    StringBuilder aline = new StringBuilder();
    aline.append("id,")
         .append("title,")
         .append("type_content")
         .append("type_social")
         .append("type_calendar")
         .append("type_forum")
         .append("type_wiki")
         .append("type_poll")
         .append("type_other")
         .append("stream_id")
         .append("stream_title")
         .append("stream_type_user")
         .append("stream_type_space")
         .append("stream_type_space");
    return aline;
  }

  protected StringBuilder activityLine(ExoSocialActivity activity) {
    // Activity identification & type
    StringBuilder aline = new StringBuilder();
    aline.append(activity.getId()).append(','); // activity ID
    aline.append(activity.getTitle()).append(','); // activity title
    encActivityType(aline, activity.getType()); // activity type: encoded
    // aline.append(activity.getAppId()).append(','); // activity app ID (what
    // is it?): TODO
    // Stream (owner)
    ActivityStream stream = activity.getActivityStream();
    aline.append(stream.getId()).append(','); // stream ID
    // aline.append(stream.getPrettyId()).append(','); // stream pretty_name
    // (need it?)
    aline.append(stream.getTitle()).append(','); // stream title
    encStreamType(aline, stream.getType().name()); // stream type (what is it?):
                                                   // encoded
    // aline.append(findStreamFocus(stream.getPrettyId())).append(','); // TODO
    // encoded stream focus
    // Poster (creator)
    String posterId = activity.getPosterId();
    aline.append(posterId).append(','); // poster ID
    Identity poster = identityManager.getIdentity(posterId, true);
    Profile posterProfile = identityManager.getProfile(poster);
    // poster full name
    aline.append(posterProfile.getFullName()).append(',');
    // poster gender: encoded
    encGender(aline, posterProfile.getGender());
    // poster job position as team membership: encoded
    encPosition(aline, posterProfile.getPosition());
    // poster is employee: encoded
    aline.append(isEmployee(posterProfile.getId()) ? '1' : '0').append(',');
    // Meta-information
    // number of likes
    aline.append(activity.getNumberOfLikes()).append(',');
    String[] commentedIds = activity.getCommentedIds();
    aline.append(commentedIds.length).append(','); // number of comments
    String[] mentionsIds = activity.getMentionedIds();
    // aline.append().append(','); //

    // TODO others isXXX
    // activityManager.getActivitiesOfUserSpacesWithListAccess(poster).
    //
    return aline;
  }

  protected void encPosition(StringBuilder aline, String position) {
    // Columns order: engineering, sales&support, marketing, management,
    // financial, other
    if (position != null) {
      position = position.toUpperCase().toLowerCase();
      if (ENGINEERING_PATTERN.matcher(position).matches()) {
        aline.append("1,0,0,0,0,0");
      } else if (SALES_PATTERN.matcher(position).matches()) {
        aline.append("0,1,0,0,0,0");
      } else if (MARKETING_PATTERN.matcher(position).matches()) {
        aline.append("0,0,1,0,0,0");
      } else if (MANAGEMENT_PATTERN.matcher(position).matches()) {
        aline.append("0,0,0,1,0,0");
      } else if (FINANCIAL_PATTERN.matcher(position).matches()) {
        aline.append("0,0,0,0,1,0");
      } else {
        aline.append("0,0,0,0,0,1");
      }
    } else {
      aline.append("0,0,0,0,0,1");
    }
  }

  protected void encGender(StringBuilder aline, String gender) {
    // Columns order: male, female
    if (gender != null) {
      if (gender.toUpperCase().toLowerCase().equals("female")) {
        aline.append("0,1");
      } else {
        aline.append("1,0");
      }
    } else {
      aline.append("1,0");
    }
  }

  protected void encStreamType(StringBuilder aline, String streamType) {
    // Columns order: user, space
    if (streamType != null) {
      if (SpaceIdentityProvider.NAME.equals(streamType)) {
        aline.append("0,1");
      } else {
        aline.append("1,0");
      }
    } else {
      aline.append("1,0");
    }
  }

  protected void encActivityType(StringBuilder aline, String activityType) {
    // Write several columns, fill the one with 1 for given type, others with 0
    //
    // Possible types: exosocial:people, exosocial:spaces,
    // cs-calendar:spaces,
    // ks-forum:spaces, ks-wiki:spaces, ks-poll:spaces, poll:spaces,
    // contents:spaces, files:spaces,
    // sharefiles:spaces, sharecontents:spaces
    // outlook:attachment etc.
    //
    // Columns order: content, social, calendar, forum, wiki, poll, other
    if (activityType != null) {
      if (activityType.indexOf("content:") > 0) {
        aline.append("1,0,0,0,0,0,0");
      } else if (activityType.indexOf("social:") > 0) {
        aline.append("0,1,0,0,0,0,0");
      } else if (activityType.indexOf("calendar:") > 0) {
        aline.append("0,0,1,0,0,0,0");
      } else if (activityType.indexOf("forum:") > 0) {
        aline.append("0,0,0,1,0,0,0");
      } else if (activityType.indexOf("wiki:") > 0) {
        aline.append("0,0,0,0,1,0,0");
      } else if (activityType.indexOf("poll:") > 0) {
        aline.append("0,0,0,0,0,1,0");
      } else {
        aline.append("0,0,0,0,0,0,1");
      }
    } else {
      aline.append("0,0,0,0,0,0,1");
    }
  }

  protected boolean isEmployee(String userId) {
    try {
      Collection<Group> userGroups = organization.getGroupHandler().findGroupsOfUser(userId);
      for (Group g : userGroups) {
        if (EMPLOYEE_GROUPID.equals(g.getId())) {
          return true;
        }
      }
    } catch (Exception e) {
      LOG.warn("Error getting user group: " + userId + ". Error: " + e.getMessage());
    }
    return false;
  }

}
