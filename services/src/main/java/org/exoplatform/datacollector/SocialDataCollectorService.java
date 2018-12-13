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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.picocontainer.Startable;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.datacollector.dao.ActivityCommentedDAO;
import org.exoplatform.datacollector.dao.ActivityLikedDAO;
import org.exoplatform.datacollector.dao.ActivityMentionedDAO;
import org.exoplatform.datacollector.dao.ActivityPostedDAO;
import org.exoplatform.platform.gadget.services.LoginHistory.LoginHistoryBean;
import org.exoplatform.platform.gadget.services.LoginHistory.LoginHistoryService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ActivityStream.Type;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * The Class SocialDataCollectorService.
 */
public class SocialDataCollectorService implements Startable {

  /** Logger */
  private static final Log       LOG                 = ExoLogger.getExoLogger(SocialDataCollectorService.class);

  public static final int        BATCH_SIZE          = 200;

  protected static final Pattern ENGINEERING_PATTERN =
                                                     Pattern.compile("^.*developer|architect|r&d|mobile|qa|fqa|tqa|test|quality|qualité|expert|integrator|designer|cwi|technical advisor|services delivery|software engineer.*$");

  protected static final Pattern SALES_PATTERN       =
                                               Pattern.compile("^.*consultant|sales|client|support|sales engineer|demand generator.*$");

  protected static final Pattern MARKETING_PATTERN   =
                                                   Pattern.compile("^.*brand|communication|marketing|customer success|user experience|.*$");

  protected static final Pattern MANAGEMENT_PATTERN  =
                                                    Pattern.compile("^.*officer|chief|founder|coo|cto|cio|evp|advisor|product manager|director|general manager.*$");

  protected static final Pattern FINANCIAL_PATTERN   = Pattern.compile("^.*accountant|financial|investment|account manager.*$");

  /**
   * The Constant EMPLOYEE_GROUPID - TODO better make it configurable. For
   * /Groups/spaces/exo_employees.
   */
  protected static final String  EMPLOYEE_GROUPID    = "/spaces/exo_employees";

  /**
   * The Class ActivityParticipant.
   */
  protected static class ActivityParticipant {
    final String  id;

    final Integer isConversed;

    final Integer isFavored;

    ActivityParticipant(String id, Boolean isConversed, Boolean isFavored) {
      super();
      if (id == null) {
        throw new NullPointerException("id should be not null");
      }
      this.id = id;
      if (isConversed == null) {
        throw new NullPointerException("isConversed should be not null");
      }
      this.isConversed = isConversed ? 1 : 0;
      if (isFavored == null) {
        throw new NullPointerException("isFavored should be not null");
      }
      this.isFavored = isFavored ? 1 : 0;
    }

    @Override
    public int hashCode() {
      int hc = 7 + id.hashCode() * 31;
      hc = hc * 31 + isConversed.hashCode();
      hc = hc * 31 + isFavored.hashCode();
      return hc;
    }

    @Override
    public boolean equals(Object o) {
      if (o != null) {
        if (this.getClass().isAssignableFrom(o.getClass())) {
          ActivityParticipant other = this.getClass().cast(o);
          return id.equals(other.id) && isConversed.equals(other.isConversed) && isFavored.equals(other.isFavored);
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName() + " [id=" + id + ", isConversed=" + isConversed.intValue() + ", isFavored="
          + isFavored.intValue() + "]";
    }
  }

  /**
   * Load all given access list to a {@link List} instance. Use carefully for
   * larger data or prefer using {@link #loadListIterator(ListAccess)} instead.
   *
   * @param <E> the element type
   * @param list the {@link ListAccess} instance
   * @return the {@link List} instance
   * @throws Exception the exception
   */
  public static <E> List<E> loadListAll(ListAccess<E> list) throws Exception {
    List<E> res = new ArrayList<>();
    int size = list.getSize();
    if (size == 0) {
      // here we assume that load() below will raise an error on end-of-data
      size = 1;
    }
    int batches = size / BATCH_SIZE;
    if (batches == 0) {
      batches = 1;
    }
    int batchIndex = 0;
    for (int fi = 0; fi < batches; fi++) {
      try {
        int batchSize = size - batchIndex;
        if (batchSize > 0) {
          if (batchSize >= BATCH_SIZE) {
            batchSize = BATCH_SIZE;
          }
          for (E e : list.load(batchIndex, batchSize)) {
            res.add(e);
          }
          batchIndex += batchSize;
        } else {
          // reached actual end-of-data
          break;
        }
      } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
        // faced with actual end-of-data
        LOG.warn("Unexpected index/size error in the batch:", e);
        break;
      }
    }
    return res;
  }

  /**
   * Wrap given access list into an {@link Iterator} instance.
   *
   * @param <E> the element type
   * @param list the list
   * @return the iterator
   * @throws Exception the exception
   */
  public static <E> Iterator<E> loadListIterator(ListAccess<E> list) throws Exception {
    final int size;
    int listSize = list.getSize();
    if (listSize == 0) {
      // here we assume that load() below will raise an error on end-of-data
      size = 1;
    } else {
      size = listSize;
    }

    Iterator<E> res = new Iterator<E>() {

      int batchIndex = 0;

      int index      = 0;

      E[] nextBatch;

      E   next;

      private void loadNextBatch() {
        try {
          int batchSize = size - batchIndex;
          if (batchSize > 0) {
            if (batchSize >= BATCH_SIZE) {
              batchSize = BATCH_SIZE;
            }
            nextBatch = list.load(batchIndex, batchSize);
            batchIndex += batchSize;
          } else {
            // reached actual end-of-data
            nextBatch = null;
          }
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
          // faced with actual end-of-data
          LOG.warn("Unexpected index/size error during loading access list:", e);
          nextBatch = null;
        } catch (Exception e) {
          // Here can be DB or network error
          LOG.error("Unexpected error during loading access list:", e);
          nextBatch = null;
        }
      }

      private void loadNext() {
        if (nextBatch == null) {
          loadNextBatch();
          index = 0;
        }
        if (nextBatch != null && nextBatch.length > 0) {
          next = nextBatch[index++];
          if (nextBatch.length == index) {
            nextBatch = null;
          }
        } else {
          next = null;
          nextBatch = null;
          index = 0;
        }
      }

      @Override
      public boolean hasNext() {
        if (next == null) {
          loadNext();
        }
        return next != null;
      }

      @Override
      public E next() {
        if (hasNext()) {
          E theNext = next;
          next = null;
          return theNext;
        }
        throw new NoSuchElementException("No more elements");
      }
    };
    return res;
  }

  protected final IdentityManager      identityManager;

  protected final ActivityManager      activityManager;

  protected final RelationshipManager  relationshipManager;

  protected final SpaceService         spaceService;

  protected final OrganizationService  organization;

  protected final LoginHistoryService  loginHistory;

  protected final UserACL              userACL;

  protected final ActivityCommentedDAO commentStorage;

  protected final ActivityPostedDAO    postStorage;

  protected final ActivityLikedDAO     likeStorage;

  protected final ActivityMentionedDAO mentionStorage;

  /**
   * Instantiates a new data collector service.
   *
   * @param jcrService the jcr service
   * @param sessionProviders the session providers
   * @param hierarchyCreator the hierarchy creator
   * @param organization the organization
   * @param identityManager the identity manager
   * @param activityManager the activity manager
   * @param relationshipManager the relationship manager
   * @param spaceService the space service
   * @param loginHistory the login history
   * @param userACL the user ACL
   * @param postStorage the post storage
   * @param commentStorage the comment storage
   * @param likeStorage the like storage
   * @param mentionStorage the mention storage
   */
  public SocialDataCollectorService(RepositoryService jcrService,
                                    SessionProviderService sessionProviders,
                                    NodeHierarchyCreator hierarchyCreator,
                                    OrganizationService organization,
                                    IdentityManager identityManager,
                                    ActivityManager activityManager,
                                    RelationshipManager relationshipManager,
                                    SpaceService spaceService,
                                    LoginHistoryService loginHistory,
                                    UserACL userACL,
                                    ActivityPostedDAO postStorage,
                                    ActivityCommentedDAO commentStorage,
                                    ActivityLikedDAO likeStorage,
                                    ActivityMentionedDAO mentionStorage) {
    super();
    this.identityManager = identityManager;
    this.activityManager = activityManager;
    this.relationshipManager = relationshipManager;
    this.organization = organization;
    this.spaceService = spaceService;
    this.loginHistory = loginHistory;
    this.userACL = userACL;

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
  protected void collectUserActivities(PrintWriter out) throws IllegalArgumentException, Exception {
    // ProfileFilter filter = new ProfileFilter();
    // long idsCount =
    // identityStorage.getIdentitiesByProfileFilterCount(OrganizationIdentityProvider.NAME,
    // filter);
    Iterator<Identity> idIter = loadListIterator(identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME,
                                                                                              new ProfileFilter(),
                                                                                              true));
    while (idIter.hasNext()) {
      // FYI flag for profile loading not used inside the call
      Identity id = idIter.next();

      // Find this user favorite participants (influencers) and streams
      List<Identity> idConnections = loadListAll(relationshipManager.getConnections(id));

      List<Space> userSpaces = loadListAll(spaceService.getMemberSpaces(id.getRemoteId()));
      UserInfluencers influencers = new UserInfluencers(id, idConnections, userSpaces);

      influencers.addCommentedPoster(commentStorage.findPartIsCommentedPoster(id.getId()));
      influencers.addCommentedCommenter(commentStorage.findPartIsCommentedCommenter(id.getId()));
      influencers.addCommentedConvoPoster(commentStorage.findPartIsCommentedConvoPoster(id.getId()));

      influencers.addPostCommenter(commentStorage.findPartIsPostCommenter(id.getId()));
      influencers.addCommentCommenter(commentStorage.findPartIsCommentCommenter(id.getId()));
      influencers.addConvoCommenter(commentStorage.findPartIsConvoCommenter(id.getId()));

      influencers.addMentioner(mentionStorage.findPartIsMentioner(id.getId()));
      influencers.addMentioned(mentionStorage.findPartIsMentioned(id.getId()));

      influencers.addLikedPoster(likeStorage.findPartIsLikedPoster(id.getId()));
      influencers.addLikedCommenter(likeStorage.findPartIsLikedCommenter(id.getId()));
      influencers.addLikedConvoPoster(likeStorage.findPartIsLikedConvoPoster(id.getId()));

      influencers.addPostLiker(likeStorage.findPartIsPostLiker(id.getId()));
      influencers.addCommentLiker(likeStorage.findPartIsCommentLiker(id.getId()));
      influencers.addConvoLiker(likeStorage.findPartIsConvoLiker(id.getId()));

      influencers.addSamePostLiker(likeStorage.findPartIsSamePostLiker(id.getId()));
      influencers.addSameCommentLiker(likeStorage.findPartIsSameCommentLiker(id.getId()));
      influencers.addSameConvoLiker(likeStorage.findPartIsSameConvoLiker(id.getId()));

      // Here the influencers object knows favorite streams of the user
      Collection<String> userStreams = influencers.getFavoriteStreamsTop(10);
      if (userStreams.size() < 10) {
        // TODO add required (to 10) streams where user has most of its
        // connections
      }
      if (userStreams.size() > 0) {
        influencers.addStreamPoster(postStorage.findPartIsFavoriteStreamPoster(id.getId(), userStreams));
        influencers.addStreamCommenter(commentStorage.findPartIsFavoriteStreamCommenter(id.getId(), userStreams));
        influencers.addStreamPostLiker(likeStorage.findPartIsFavoriteStreamPostLiker(id.getId(), userStreams));
        influencers.addStreamCommentLiker(likeStorage.findPartIsFavoriteStreamCommentLiker(id.getId(), userStreams));
      }

      //
      // RealtimeListAccess<ExoSocialActivity> spacesActivities =
      // activityManager.getActivitiesOfUserSpacesWithListAccess(id);
      //
      // spacesActivities.loadNewer(sinceTime, limit);
      // spacesActivities.getNumberOfNewer(sinceTime);
      // RealtimeListAccess<ExoSocialActivity> connsActivities =
      // activityManager.getActivitiesOfConnectionsWithListAccess(id);
      //
      // activityManager.getActivitiesWithListAccess(ownerIdentity,
      // viewerIdentity)

      out.println(activityHeader());
      // load identity's activities and collect its data
      Iterator<ExoSocialActivity> feedIter = loadListIterator(activityManager.getActivityFeedWithListAccess(id));
      while (feedIter.hasNext()) {
        ExoSocialActivity activity = feedIter.next();
        try {
          out.println(activityLine(influencers, activity));
        } catch (ActivityDataException e) {
          LOG.warn(e);
        }
      }
    }
  }

  protected String activityHeader() {
    StringBuilder aline = new StringBuilder();
    aline.append("id,")
         .append("title,")
         .append("type_content,")
         .append("type_social,")
         .append("type_calendar,")
         .append("type_forum,")
         .append("type_wiki,")
         .append("type_poll,")
         .append("type_other,")
         .append("owner_id,")
         .append("owner_title,")
         .append("owner_type_organization,")
         .append("owner_type_space,")
         .append("owner_influence,")
         .append("number_of_likes,")
         .append("number_of_comments,")
         .append("reactivity,")
         .append("is_mentions_me,")
         .append("is_mentions_connections,")
         .append("is_commented_by_me,")
         .append("is_commented_by_connetions,")
         .append("is_liked_by_me,")
         .append("is_liked_by_connections,")
         // Poster features
         .append("poster_id,")
         .append("poster_gender,")
         .append("poster_is_employee,")
         .append("poster_is_lead,")
         .append("poster_is_in_connections,")
         .append("poster_focus_engineering,")
         .append("poster_focus_sales,")
         .append("poster_focus_marketing,")
         .append("poster_focus_management,")
         .append("poster_focus_financial,")
         .append("poster_focus_other,")
         .append("poster_influence,")
         // Participant #1 features
         .append("participant1_id,")
         .append("participant1_conversed,")
         .append("participant1_favored,")
         .append("participant1_gender,")
         .append("participant1_is_employee,")
         .append("participant1_is_lead,")
         .append("participant1_is_in_connections,")
         .append("participant1_focus_engineering,")
         .append("participant1_focus_sales,")
         .append("participant1_focus_marketing,")
         .append("participant1_focus_management,")
         .append("participant1_focus_financial,")
         .append("participant1_focus_other,")
         .append("participant1_influence,")
         // Participant #2 features
         .append("participant2_id,")
         .append("participant2_conversed,")
         .append("participant2_favored,")
         .append("participant2_gender,")
         .append("participant2_is_employee,")
         .append("participant2_is_lead,")
         .append("participant2_is_in_connections,")
         .append("participant2_focus_engineering,")
         .append("participant2_focus_sales,")
         .append("participant2_focus_marketing,")
         .append("participant2_focus_management,")
         .append("participant2_focus_financial,")
         .append("participant2_focus_other,")
         .append("participant2_influence,")
         // Participant #3 features
         .append("participant3_id,")
         .append("participant3_conversed,")
         .append("participant3_favored,")
         .append("participant3_gender,")
         .append("participant3_is_employee,")
         .append("participant3_is_lead,")
         .append("participant3_is_in_connections,")
         .append("participant3_focus_engineering,")
         .append("participant3_focus_sales,")
         .append("participant3_focus_marketing,")
         .append("participant3_focus_management,")
         .append("participant3_focus_financial,")
         .append("participant3_focus_other,")
         .append("participant3_influence,")
         // Participant #4 features
         .append("participant4_id,")
         .append("participant4_conversed,")
         .append("participant4_favored,")
         .append("participant4_gender,")
         .append("participant4_is_employee,")
         .append("participant4_is_lead,")
         .append("participant4_is_in_connections,")
         .append("participant4_focus_engineering,")
         .append("participant4_focus_sales,")
         .append("participant4_focus_marketing,")
         .append("participant4_focus_management,")
         .append("participant4_focus_financial,")
         .append("participant4_focus_other,")
         .append("participant4_influence,")
         // Participant #5 features
         .append("participant5_id,")
         .append("participant5_conversed,")
         .append("participant5_favored,")
         .append("participant5_gender,")
         .append("participant5_is_employee,")
         .append("participant5_is_lead,")
         .append("participant5_is_in_connections,")
         .append("participant5_focus_engineering,")
         .append("participant5_focus_sales,")
         .append("participant5_focus_marketing,")
         .append("participant5_focus_management,")
         .append("participant5_focus_financial,")
         .append("participant5_focus_other,")
         .append("participant5_influence\n");
    return aline.toString();
  }

  protected String activityLine(UserInfluencers influencers, ExoSocialActivity activity) throws ActivityDataException {
    // Activity identification & type
    StringBuilder aline = new StringBuilder();
    // ID
    aline.append(activity.getId()).append(',');
    // title
    aline.append(activity.getTitle()).append(',');
    // type: encoded
    encActivityType(aline, activity.getType());
    // app ID: TODO need it?
    // aline.append(activity.getAppId()).append(',');
    Identity ownerId;
    ActivityStream stream = activity.getActivityStream();
    boolean isSpace = Type.SPACE.equals(stream.getType());
    if (isSpace) {
      ownerId = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, activity.getStreamOwner(), false);
    } else {
      ownerId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, activity.getStreamOwner(), false);
    }
    // owner ID
    if (ownerId == null) {
      throw new ActivityDataException("Cannot find social identity of stream owner: " + activity.getStreamOwner()
          + ". Activity will be skipped: " + activity.getId());
    }
    aline.append(ownerId.getId()).append(',');
    // owner type
    if (isSpace) {
      aline.append("0,1,");
    } else {
      aline.append("1,0,");
    }
    // TODO do we need owner/stream focus?
    // aline.append(findStreamFocus(stream.getPrettyId())).append(',');
    // owner influence
    aline.append(influencers.getStreamWeight(ownerId.getId())).append(',');
    // number_of_likes
    aline.append(activity.getNumberOfLikes()).append(',');
    // number_of_comments
    aline.append(activity.getCommentedIds().length).append(',');
    // reactivity: difference in days between a day of posted and a day when
    // user commented/liked: 0..1, where 1 is same day, 0 is 30+ days old
    // TODO Should we take in account user login history: time between nearest
    // login and the reaction? Indeed this may be not accurate.
    aline.append(influencers.getPostReactivity(activity.getId())).append(',');

    final String myId = influencers.getUserIdentity().getId();
    final Collection<String> myConns = influencers.getUserConnections().keySet();

    // is_mentions_me
    // is_mentions_connections
    encContainsMeOthers(aline, myId, myConns, activity.getMentionedIds());

    // is_commented_by_me
    // is_commented_by_connetions
    encContainsMeOthers(aline, myId, myConns, activity.getCommentedIds());

    // is_liked_by_me
    // is_liked_by_connections
    encContainsMeOthers(aline, myId, myConns, activity.getLikeIdentityIds());

    // Poster (creator)
    String posterId = activity.getPosterId();
    aline.append(posterId).append(','); // poster ID
    Identity poster = identityManager.getIdentity(posterId, false);
    if (poster == null) {
      throw new ActivityDataException("Cannot find social identity of activity poster: " + posterId
          + ". Activity will be skipped: " + activity.getId());
    }
    Profile posterProfile = identityManager.getProfile(poster);
    if (posterProfile == null) {
      throw new ActivityDataException("Cannot find profile of activity poster: " + posterId + " (" + poster.getRemoteId() + ")"
          + ". Activity will be skipped: " + activity.getId());
    }
    // TODO poster full name ?
    // aline.append(posterProfile.getFullName()).append(',');
    // poster gender: encoded
    encGender(aline, posterProfile.getGender());
    // poster_is_employee
    aline.append(isEmployee(posterProfile.getId()) ? '1' : '0').append(',');
    // TODO poster_is_lead
    aline.append('0').append(',');
    // poster_is_in_connections
    aline.append(myConns.contains(posterId) ? '1' : '0').append(',');
    // poster_focus_*: poster job position as team membership encoded
    encPosition(aline, posterProfile.getPosition());
    // poster_influence
    aline.append(influencers.getParticipantWeight(posterId, ownerId.getId())).append(',');

    // Find top 5 participants in this activity, we need not less than 5!
    for (ActivityParticipant p : findTopParticipants(activity, influencers, isSpace, 5)) {
      // participantN_id
      aline.append(p.id).append(',');
      // participantN_conversed
      aline.append(p.isConversed).append(',');
      // participantN_favored
      aline.append(p.isFavored).append(',');
      //
      Identity part = identityManager.getIdentity(p.id, false);
      if (part == null) {
        throw new ActivityDataException("Cannot find social identity of activity participant: " + p.id
            + ". Activity will be skipped: " + activity.getId());
      }
      Profile partProfile = identityManager.getProfile(part);
      if (partProfile == null) {
        throw new ActivityDataException("Cannot find profile of activity participant: " + p.id + " (" + part.getRemoteId() + ")"
            + ". Activity will be skipped: " + activity.getId());
      }
      // participantN_gender: encoded
      encGender(aline, partProfile.getGender());
      // participantN_is_employee
      aline.append(isEmployee(partProfile.getId()) ? '1' : '0').append(',');
      // TODO participantN_is_lead
      aline.append('0').append(',');
      // participantN_is_in_connections
      aline.append(myConns.contains(p.id) ? '1' : '0').append(',');
      // participantN_focus_*: job position as team membership encoded
      encPosition(aline, partProfile.getPosition());
      // participantN_influence
      aline.append(influencers.getParticipantWeight(p.id, ownerId.getId())).append(',');
    }
    
    aline.setCharAt(aline.length() - 1, '\n'); // end of line

    return aline.toString();
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

  protected void encContainsMeOthers(StringBuilder aline, String me, Collection<String> others, String[] target) {
    int isMe = 0;
    int isOthers = 0;
    for (String o : target) {
      if (me.equals(o)) {
        isMe = 1;
      }
      if (others.contains(o)) {
        isOthers = 1;
      }
      if (isMe == 1 && isOthers == 1) {
        break; // Make loop quicker if already know everything
      }
    }
    aline.append(isMe).append(',');
    aline.append(isOthers).append(',');
  }

  protected Collection<ActivityParticipant> findTopParticipants(ExoSocialActivity activity,
                                                                UserInfluencers influencers,
                                                                boolean isInSpace,
                                                                int topLength) {
    Map<String, ActivityParticipant> top = new LinkedHashMap<>();
    // 1) Commenters are first line candidates:
    Iterator<String> piter = Arrays.asList(activity.getCommentedIds()).iterator();
    while (piter.hasNext() && top.size() < topLength) {
      top.computeIfAbsent(piter.next(), p -> new ActivityParticipant(p, true, false));
    }
    if (top.size() < topLength) {
      // 2) Mentioned are second line (count them as passive commenters)
      piter = Arrays.asList(activity.getMentionedIds()).iterator();
      while (piter.hasNext() && top.size() < topLength) {
        top.computeIfAbsent(piter.next(), p -> new ActivityParticipant(p, true, false));
      }
    }
    if (top.size() < topLength) {
      // 3) Liked are third line
      piter = Arrays.asList(activity.getLikeIdentityIds()).iterator();
      while (piter.hasNext() && top.size() < topLength) {
        top.computeIfAbsent(piter.next(), p -> new ActivityParticipant(p, false, true));
      }
    }
    if (top.size() < topLength) {
      // 4) Viewers are fourth line: find connection/space viewers for given
      // user:
      // 4.1) First try over user connections/space-members who can
      // access the activity stream and last active around the activity date -
      // this seems a heavy op.

      // TODO attempt to select most relevant connections/members (use another
      // ML to figure out 'leaders' in the intranet/spaces)

      // Count on user history for 30 days after the activity
      long beforeTime = 15 * 60 * 1000; // 15min in ms
      long activityDate = activity.getPostedTime();
      long activityScopeTimeBegin = activityDate - beforeTime;
      long activityScopeTimeEnd = activityScopeTimeBegin
          + (UserInfluencers.DAY_LENGTH_MILLIS * UserInfluencers.REACTIVITY_DAYS_RANGE);

      if (isInSpace) {
        // Get space managers and members who were logged-in
        Space space = spaceService.getSpaceByGroupId(activity.getStreamOwner());
        if (space != null) {
          String[] smanagers = space.getManagers();
          for (int i = 0; i < smanagers.length && top.size() < topLength; i++) {
            if (wasUserLoggedin(smanagers[i], activityScopeTimeBegin, activityScopeTimeEnd)) {
              top.computeIfAbsent(smanagers[i], p -> new ActivityParticipant(p, false, false));
            }
          }
          String[] smembers = space.getMembers();
          for (int i = 0; i < smembers.length && top.size() < topLength; i++) {
            if (wasUserLoggedin(smembers[i], activityScopeTimeBegin, activityScopeTimeEnd)) {
              top.computeIfAbsent(smembers[i], p -> new ActivityParticipant(p, false, false));
            }
          }
          // if top still not full, we add managers w/o login check
          for (int i = 0; i < smanagers.length && top.size() < topLength; i++) {
            top.computeIfAbsent(smanagers[i], p -> new ActivityParticipant(p, false, false));
          }
        }
      }

      if (top.size() < topLength) {
        // Add user connections who were logged-in
        Iterator<String> citer = influencers.getUserConnections().keySet().iterator();
        while (citer.hasNext() && top.size() < topLength) {
          String cid = citer.next();
          if (wasUserLoggedin(cid, activityScopeTimeBegin, activityScopeTimeEnd)) {
            top.computeIfAbsent(cid, p -> new ActivityParticipant(p, false, false));
          }
        }
      }

      if (top.size() < topLength) {
        // TODO 4.2) Second and finally, if user has no spaces/connections,
        // * then we choose user's group managers
        // TODO first use Employees group (or similar)
        Collection<Group> userGroups;
        try {
          userGroups = organization.getGroupHandler().findGroupsOfUser(influencers.getUserIdentity().getRemoteId());
        } catch (Exception e) {
          LOG.warn("Error reading user groups for " + influencers.getUserIdentity().getRemoteId(), e);
          userGroups = null;
        }
        if (userGroups != null) {
          for (Group ug : userGroups) {
            String gid = ug.getId();
            if (!gid.startsWith(SpaceUtils.SPACE_GROUP)) {
              // Use non space groups
              String managerMemebership = getMembershipName("manager");
              if (managerMemebership != null) {
                Iterator<String> miter = SpaceUtils.findMembershipUsersByGroupAndTypes(gid, managerMemebership).iterator();
                // TODO cleanup
                // Iterator<User> gusers =
                // loadListIterator(organization.getUserHandler().findUsersByGroupId(gid));
                while (miter.hasNext() && top.size() < topLength) {
                  // String userId = gusers.next().getUserName();
                  String uname = miter.next();
                  Identity mid = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, uname, false);
                  if (mid != null) {
                    top.computeIfAbsent(mid.getId(), p -> new ActivityParticipant(p, false, false));
                  } else {
                    LOG.warn("Group '" + gid + "' manager identity cannot be found in Social: " + uname);
                  }
                }
              }
            }
          }
        }
        // * then we choose Root user and members of Admins group,
        if (top.size() < topLength) {
          Identity sid = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userACL.getSuperUser(), false);
          if (sid != null) {
            top.computeIfAbsent(sid.getId(), p -> new ActivityParticipant(p, false, false));
          } else {
            LOG.warn("Root user identity cannot be found in Social");
          }
          // organization.getGroupHandler().findGroupById(userACL.getAdminGroups());
          Iterator<String> aiter = SpaceUtils.findMembershipUsersByGroupAndTypes(userACL.getAdminGroups(), "*").iterator();
          while (aiter.hasNext() && top.size() < topLength) {
            // String userId = gusers.next().getUserName();
            String aname = aiter.next();
            Identity aid = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, aname, false);
            if (aid != null) {
              top.computeIfAbsent(aid.getId(), p -> new ActivityParticipant(p, false, false));
            } else {
              LOG.warn("Admin group member identity cannot be found in Social: " + aname);
            }
          }
        }
        // * TODO if not enough in admins - then guess random from "similar"
        // users in
        // the organization,
        // * if no users - add "black" user with id=0.
        if (top.size() < topLength) {
          int needAdd = top.size() - topLength;
          do {
            top.computeIfAbsent("0", p -> new ActivityParticipant(p, false, false));
            needAdd--;
          } while (needAdd > 0);
        }
      }
    }
    return Collections.unmodifiableCollection(top.values());
  }

  protected boolean wasUserLoggedin(String userId, long fromTime, long toTime) {
    try {
      List<LoginHistoryBean> phistory = loginHistory.getLoginHistory(userId, fromTime, toTime);
      // List is descending order, thus we see from the last (earlier login)
      // ListIterator<LoginHistoryBean> liter =
      // phistory.listIterator(phistory.size());
      // while (liter.hasPrevious()) {
      // LoginHistoryBean hbean = liter.previous();
      // if (hbean.getLoginTime() >= activityScopeTimeBegin &&
      // hbean.getLoginTime() < activityScopeTimeEnd) {
      // return true;
      // }
      // }
      // return false;
      return phistory.size() > 0;
    } catch (Exception e) {
      LOG.warn("Error reading login history for " + userId);
      return false;
    }
  }

  /**
   * Gets organizational membership name by its type name.
   *
   * @param membershipType the membership type
   * @return the membership name
   */
  protected String getMembershipName(String membershipType) {
    try {
      return organization.getMembershipTypeHandler().findMembershipType(membershipType).getName();
    } catch (Exception e) {
      LOG.error("Error finding manager membership in organization service", e);
      return null;
    }
  }

}
