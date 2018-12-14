package org.exoplatform.datacollector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.application.SpaceActivityPublisher;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

public class BaseActivityTestCase extends BaseCommonsTestCase {

  /** Logger */
  private static final Log      LOG = ExoLogger.getExoLogger(BaseActivityTestCase.class);

  protected PortalContainer     container;

  protected SpaceService        spaceService;

  protected RelationshipManager relationshipManager;

  protected IdentityManager     identityManager;

  protected ActivityManager     activityManager;

  protected String              johnId, maryId, jamesId, jasonId, jackId, aliceId, bobId, marketingId, salesId, engineeringId,
      productId, supportId;

  @Override
  protected void beforeClass() {
    super.beforeClass();

    this.container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);

    spaceService = (SpaceService) container.getComponentInstanceOfType(SpaceService.class);
    relationshipManager = (RelationshipManager) container.getComponentInstanceOfType(RelationshipManager.class);
    identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);
    activityManager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
    initSpaces();
    initRelations();
    // Before creating test activites clean all auto-generated by spaces and
    // relations,
    // this also will clean all what was created by previous tests.
    cleanActivities();
    initActivities();

    // Initializes users and spaces IDs
    initIdentitiesIds();
    // TODO this will not work for multi-threading execution (see forkCount in
    // pom.xml)
  }

  @Override
  protected void afterClass() {
    super.afterClass();
    RequestLifeCycle.end();
  }

  /******** internals *********/

  /**
   * Initializes testing spaces
   */
  protected void initSpaces() {
    if (spaceService.getSpaceByDisplayName(TestUtils.ENGINEERING_TEAM) == null) {
      createSpace(TestUtils.ENGINEERING_TEAM, TestUtils.ENGINERING_MANAGERS, TestUtils.ENGINERING_MEMBERS);
    }
    if (spaceService.getSpaceByDisplayName(TestUtils.MARKETING_TEAM) == null) {
      createSpace(TestUtils.MARKETING_TEAM, TestUtils.MARKETING_MANAGERS, TestUtils.MARKETING_MEMBERS);
    }
    if (spaceService.getSpaceByDisplayName(TestUtils.PRODUCT_TEAM) == null) {
      createSpace(TestUtils.PRODUCT_TEAM, TestUtils.PRODUCT_MANAGERS, TestUtils.PRODUCT_MEMBERS);
    }
    if (spaceService.getSpaceByDisplayName(TestUtils.SALES_TEAM) == null) {
      createSpace(TestUtils.SALES_TEAM, TestUtils.SALES_MANAGERS, TestUtils.SALES_MEMBERS);
    }
    if (spaceService.getSpaceByDisplayName(TestUtils.SUPPORT_TEAM) == null) {
      createSpace(TestUtils.SUPPORT_TEAM, TestUtils.SUPPORT_MANAGERS, TestUtils.SUPPORT_MEMBERS);
    }
  }

  /**
   * Initializes space and user identities ID's
   */
  protected void initIdentitiesIds() {
    // It's current user in tests
    jasonId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "jason", true).getId();
    // Other users
    johnId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "john", true).getId();
    jamesId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "james", true).getId();
    maryId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "mary", true).getId();
    jackId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "jack", true).getId();
    aliceId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "alice", true).getId();
    bobId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "bob", true).getId();
    // Spaces
    marketingId = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, "marketing_team", true).getId();
    supportId = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, "support_team", true).getId();
    productId = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, "product_team", true).getId();
    engineeringId = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, "engineering_team", true).getId();
    salesId = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, "sales_team", true).getId();
  }

  /**
   * Creates a space
   * 
   * @param displayName space displayName
   * @param managers managers of the space
   * @param members members of the space
   */
  protected void createSpace(String displayName, String[] managers, String[] members) {
    Space space = new Space();
    space.setDisplayName(displayName);
    space.setPrettyName(displayName);
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space" + displayName);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.PUBLIC);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    String groupId = null;
    try {
      groupId = SpaceUtils.createGroup(space.getDisplayName(), space.getPrettyName(), managers[0]);
    } catch (SpaceException e) {
      LOG.error("Error creating space group", e);
    }

    space.setGroupId(groupId);
    space.setUrl(space.getPrettyName());
    String[] invitedUsers = new String[] {};
    String[] pendingUsers = new String[] {};
    space.setInvitedUsers(invitedUsers);
    space.setPendingUsers(pendingUsers);
    space.setManagers(managers);
    space.setMembers(members);
    try {
      spaceService.saveSpace(space, true);
    } catch (SpaceException e) {
      LOG.error("Error saving space", e);
    }
  }

  /**
   * Cleans testing spaces
   */
  @Deprecated // TODO not used
  protected void cleanSpaces() {
    Space space = spaceService.getSpaceByDisplayName(TestUtils.ENGINEERING_TEAM);
    if (space != null) {
      spaceService.deleteSpace(space);
    }
    space = spaceService.getSpaceByDisplayName(TestUtils.MARKETING_TEAM);
    if (space != null) {
      spaceService.deleteSpace(space);
    }
    space = spaceService.getSpaceByDisplayName(TestUtils.PRODUCT_TEAM);
    if (space != null) {
      spaceService.deleteSpace(space);
    }
    space = spaceService.getSpaceByDisplayName(TestUtils.SALES_TEAM);
    if (space != null) {
      spaceService.deleteSpace(space);
    }
    space = spaceService.getSpaceByDisplayName(TestUtils.SUPPORT_TEAM);
    if (space != null) {
      spaceService.deleteSpace(space);
    }
  }

  /**
   * Initializes testing activities from JSON (TestUtils.TESTING_DATA_FILENAME)
   */
  protected void initActivities() {
    JSONObject json = TestUtils.getJSON(TestUtils.TESTING_DATA_FILENAME);
    try {
      JSONArray activities = json.getJSONObject("data").getJSONArray("activities");
      RequestLifeCycle.begin(PortalContainer.getInstance());
      for (int i = 0; i < activities.length(); i++) {
        try {
          JSONObject activity = activities.getJSONObject(i);
          Thread.sleep(50);
          createActivity(activity);
        } catch (Exception e) {
          LOG.error("Error when creating activity number " + i, e);
        }
      }
    } catch (JSONException e) {
      LOG.error("Error initializing activities", e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  /**
   * Create an activity.
   *
   * @param activityJSON the activity JSON
   * @throws Exception the exception
   */
  protected void createActivity(JSONObject activityJSON) throws Exception {
    String poster = activityJSON.getString("poster");
    String space = activityJSON.has("space") ? activityJSON.getString("space") : null;
    String body = activityJSON.getString("body");
    String title = activityJSON.getString("title");
    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, poster, true);

    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle(title);
    activity.setBody(body);
    activity.setUserId(userIdentity.getId());
    if (space != null) {
      Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space, true);
      activity.setType(SpaceActivityPublisher.SPACE_APP_ID);
      activityManager.saveActivityNoReturn(spaceIdentity, activity);

    } else {
      activity.setType(TestUtils.DEFAULT_ACTIVITY);
      activityManager.saveActivityNoReturn(userIdentity, activity);
    }

    activity.setPermanLink(LinkProvider.getSingleActivityUrl(activity.getId()));

    // Wait before adding activity items (for async operations in Social)
    Thread.sleep(250);
    // Likes
    if (activityJSON.optJSONArray("likes") != null) {
      initLikes(activityJSON.getJSONArray("likes"), activity);
    }

    // Comments
    if (activityJSON.optJSONArray("comments") != null) {
      initComments(activity, activityJSON);
    }

  }

  protected void initComments(ExoSocialActivity activity, JSONObject activityJSON) throws Exception {

    JSONArray comments = activityJSON.getJSONArray("comments");

    for (int i = 0; i < comments.length(); i++) {
      JSONObject commentJSON = comments.getJSONObject(i);

      Thread.sleep(100);
      Identity identityComment = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                     commentJSON.getString("poster"),
                                                                     false);
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle(commentJSON.getString("body"));
      comment.setUserId(identityComment.getId());
      activityManager.saveComment(activity, comment);

      if (commentJSON.optJSONArray("likes") != null) {
        initLikes(commentJSON.getJSONArray("likes"), comment);
      }

      // replies
      if (commentJSON.optJSONArray("replies") != null) {
        JSONArray replies = commentJSON.getJSONArray("replies");
        for (int j = 0; j < replies.length(); j++) {
          JSONObject replyJSON = replies.getJSONObject(j);
          Thread.sleep(50);
          Identity identityReply = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                       replyJSON.getString("poster"),
                                                                       false);
          ExoSocialActivity reply = new ExoSocialActivityImpl();
          reply.setTitle(replyJSON.getString("body"));
          reply.setUserId(identityReply.getId());
          reply.setParentCommentId(comment.getId());

          activityManager.saveComment(activity, reply);
          if (replyJSON.optJSONArray("likes") != null) {
            initLikes(replyJSON.getJSONArray("likes"), reply);
          }
        }
      }
    }
  }

  /**
   * Initializes likes inside an activity or comment
   * 
   * @param likes JSONArray with names of likers
   * @param activity that is liked
   * @throws InterruptedException
   * @throws JSONException
   */
  protected void initLikes(JSONArray likes, ExoSocialActivity activity) throws InterruptedException {
    for (int j = 0; j < likes.length(); j++) {
      Thread.sleep(100);
      String like = null;
      try {
        like = likes.getString(j);
        Identity identityLike = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, like, false);
        activityManager.saveLike(activity, identityLike);
      } catch (Exception e) {
        LOG.error("Error when liking an comment with " + like, e);
      }
    }
  }

  /**
   * Initializes testing relations between users from JSON
   * (TestUtils.TESTING_DATA_FILENAME)
   */
  protected void initRelations() {
    JSONObject json = TestUtils.getJSON(TestUtils.TESTING_DATA_FILENAME);
    JSONArray relations = null;

    try {
      relations = json.getJSONObject("data").getJSONArray("relations");
    } catch (JSONException e1) {
      LOG.error("Error when reading relations from json");
      return;
    }

    for (int i = 0; i < relations.length(); i++) {
      RequestLifeCycle.begin(PortalContainer.getInstance());

      try {
        JSONObject relation = relations.getJSONObject(i);
        Identity idInviting = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                  relation.getString("inviting"),
                                                                  false);
        Identity idInvited = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                 relation.getString("invited"),
                                                                 false);
        relationshipManager.inviteToConnect(idInviting, idInvited);
        if (relation.has("confirmed") && relation.getBoolean("confirmed")) {
          relationshipManager.confirm(idInvited, idInviting);
        }
      } catch (JSONException e) {
        LOG.error("Syntax error on relation number " + i, e);
      } finally {
        RequestLifeCycle.end();
      }
    }
  }

  /**
   * Deletes testing activities
   */
  protected void cleanActivities() {
    activityManager.getAllActivitiesWithListAccess().loadAsList(0, 100000).forEach(activityManager::deleteActivity);
  }

}
