package org.exoplatform.datacollector.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.datacollector.TestUtils;
import org.exoplatform.datacollector.domain.ActivityCommentedEntity;
import org.exoplatform.datacollector.domain.ActivityLikedEntity;
import org.exoplatform.datacollector.domain.ActivityMentionedEntity;
import org.exoplatform.datacollector.domain.ActivityPostedEntity;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ActivityStream.Type;
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

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class ActivityDAOTest extends BaseCommonsTestCase {

  /** Logger */
  private static final Log     LOG           = ExoLogger.getExoLogger(ActivityDAOTest.class);

  private ActivityCommentedDAO activityCommentDAO;

  private ActivityPostedDAO    activityPostedDAO;

  private ActivityMentionedDAO activityMentionedDAO;

  private ActivityLikedDAO     activityLikedDAO;

  private SpaceService         spaceService;

  private RelationshipManager  relationshipManager;

  private IdentityManager      identityManager;

  private ActivityManager      activityManager;

  private Identity             johnId, maryId, jamesId, jasonId, marketingId, supportId;

  private List<String>         activitiesIds = new ArrayList<String>();

  @Override
  protected void beforeClass() {
    super.beforeClass();
    PortalContainer container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);

    activityCommentDAO = (ActivityCommentedDAO) container.getComponentInstanceOfType(ActivityCommentedDAO.class);
    activityPostedDAO = (ActivityPostedDAO) container.getComponentInstance(ActivityPostedDAO.class);
    activityMentionedDAO = (ActivityMentionedDAO) container.getComponentInstance(ActivityMentionedDAO.class);
    activityLikedDAO = (ActivityLikedDAO) container.getComponentInstanceOfType(ActivityLikedDAO.class);
    spaceService = (SpaceService) container.getComponentInstanceOfType(SpaceService.class);
    relationshipManager = (RelationshipManager) container.getComponentInstanceOfType(RelationshipManager.class);
    identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);
    activityManager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
    initSpaces();
    initActivities();
    initRelations();

    // It's current user in tests
    jasonId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "jason", true);
    // Other users
    johnId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "john", true);
    jamesId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "james", true);
    maryId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "mary", true);
    // Spaces
    marketingId = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, "marketing_team", true);
    supportId = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, "support_team", true);

    // TODO this will not work for multi-threading execution (see forkCount in
    // pom.xml)
  }

  @Test
  public void testFindPartIsCommentedPoster() {
    List<ActivityCommentedEntity> res = activityCommentDAO.findPartIsCommentedPoster(johnId.getId());
    assertEquals(1, res.size());
    assertEquals(maryId.getId(), res.get(0).getPosterId());
  }

  @Test
  public void testFindPartIsCommentedCommenter() {
    assertTrue(activityCommentDAO.findPartIsCommentedCommenter(johnId.getId()).isEmpty());
  }

  @Test
  public void testFindPartIsCommentedConvoPoster() {
    assertTrue(activityCommentDAO.findPartIsCommentedConvoPoster(johnId.getId()).isEmpty());
  }

  @Test
  public void testFindUserPosts() {
    List<ActivityPostedEntity> res = activityPostedDAO.findUserPosts(maryId.getId());
    assertEquals(4, res.size());
    assertEquals(2, res.stream().filter(entity -> entity.getProviderId().equals(Type.SPACE.toString())).count());
    assertEquals(2, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  @Test
  public void testFindPartIsFavoriteStreamPoster() {
    List<ActivityPostedEntity> res = activityPostedDAO.findPartIsFavoriteStreamPoster(jasonId.getId(),
                                                                                      Arrays.asList(supportId.getId(),
                                                                                                    marketingId.getId()));
    assertEquals(2, res.size());
    assertTrue(res.stream().allMatch(entity -> !entity.getPosterId().equals(jasonId.getId())));
  }

  @Test
  public void testfindPartIsMentioned() {
    List<ActivityMentionedEntity> res = activityMentionedDAO.findPartIsMentioned(jasonId.getId());
    assertEquals(3, res.size());
    assertTrue(res.stream().allMatch(entity -> entity.getPosterId().equals(jasonId.getId())));
  }

  @Test
  public void testFindPartIsMentioner() {
    List<ActivityMentionedEntity> res = activityMentionedDAO.findPartIsMentioner(jamesId.getId());
    assertEquals(4, res.size());
    assertTrue(res.stream().allMatch(entity -> entity.getMentionedId().equals(jamesId.getId())));
  }

  // ActivityLikedEntity tests

  @Test
  public void testFindPartIsLikedPoster() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsLikedPoster(johnId.getId());
    assertEquals(3, res.size());
    assertTrue(res.stream().allMatch(entity -> entity.getLikerId().equals(johnId.getId())));
  }

  @Test
  public void testFindPartIsLikedCommenter() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsLikedCommenter(johnId.getId());
    assertEquals(3, res.size());
    assertTrue(res.stream().allMatch(entity -> entity.getLikerId().equals(johnId.getId())));
  }

  @Test
  public void testFindPartIsLikedConvoPoster() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsLikedConvoPoster(johnId.getId());
    assertEquals(2, res.size());
    assertTrue(res.stream().allMatch(entity -> entity.getLikerId().equals(johnId.getId())));
  }

  @Test
  public void testFindPartIsPostLiker() {
    // If we add jack's like to any mary's post and expect res.size() == 5 and
    // one element has likerId equal to jackId, we will fail the test.
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsPostLiker(maryId.getId());
    assertEquals(4, res.size());
    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(johnId.getId())).count());
    assertEquals(1, res.stream().filter(entity -> entity.getLikerId().equals(jasonId.getId())).count());
    assertTrue(res.stream().allMatch(entity -> entity.getPosterId().equals(maryId.getId())));
  }

  @Test
  public void testFindPartIsCommentLiker() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsCommentLiker(jasonId.getId());
    assertEquals(5, res.size());
    // TODO: fix
    LOG.info("JASON ID: " + jasonId.getId());
    LOG.info("MARY ID: " + maryId.getId());
    LOG.info("JAMES ID: " + jamesId.getId());
    LOG.info("JOHN ID: " + johnId.getId());
    res.forEach(entity -> LOG.info("Liker: " + entity.getLikerId() + " POSTER: " + entity.getPosterId() + " "
        + entity.getPosted()));
    /*
     * assertEquals(2 ,res.stream().filter(entity ->
     * entity.getLikerId().equals(maryId.getId())).count()); assertEquals(2
     * ,res.stream().filter(entity ->
     * entity.getLikerId().equals(johnId.getId())).count()); assertEquals(1
     * ,res.stream().filter(entity ->
     * entity.getLikerId().equals(jamesId.getId())).count());
     */

  }

  @Test
  public void testFindPartIsConvoLiker() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsConvoLiker(jasonId.getId());
    assertEquals(3, res.size());
    assertEquals(1, res.stream().filter(entity -> entity.getLikerId().equals(johnId.getId())).count());
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(maryId.getId())).count());
  }

  @Override
  protected void afterClass() {
    super.afterClass();
    cleanSpaces();
    RequestLifeCycle.end();
  }

  /**
   * Initializes testing spaces
   */
  private void initSpaces() {
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
   * Creates a space
   * 
   * @param displayName space displayName
   * @param managers managers of the space
   * @param members members of the space
   */
  private void createSpace(String displayName, String[] managers, String[] members) {
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
  private void cleanSpaces() {
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
  private void initActivities() {
    JSONObject json = TestUtils.getJSON(TestUtils.TESTING_DATA_FILENAME);
    try {
      JSONArray activities = json.getJSONObject("data").getJSONArray("activities");
      RequestLifeCycle.begin(PortalContainer.getInstance());
      for (int i = 0; i < activities.length(); i++) {
        try {
          JSONObject activity = activities.getJSONObject(i);
          pushActivity(activity);
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
   * Pushes an activity.
   *
   * @param activityJSON the activity JSON
   * @throws Exception the exception
   */
  private void pushActivity(JSONObject activityJSON) throws Exception {
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

    activitiesIds.add(activity.getId());
    activity.setPermanLink(LinkProvider.getSingleActivityUrl(activity.getId()));

    Thread.sleep(100);

    // Likes

    if (activityJSON.optJSONArray("likes") != null) {
      initLikes(activityJSON.getJSONArray("likes"), activity);
    }

    // Comments
    JSONArray comments = activityJSON.getJSONArray("comments");
    for (int i = 0; i < comments.length(); i++) {
      JSONObject commentJSON = comments.getJSONObject(i);

      Thread.sleep(300);
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

    }
  }

  /**
   * Initializes likes inside an activity or comment
   * 
   * @param likes JSONArray with names of likers
   * @param activity that is liked
   * @throws JSONException
   */
  private void initLikes(JSONArray likes, ExoSocialActivity activity) {

    for (int j = 0; j < likes.length(); j++) {
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
  private void initRelations() {
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

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    cleanActivities();
  }

  /**
   * Deletes testing activities
   */
  private void cleanActivities() {
    activitiesIds.forEach(activityManager::deleteActivity);
    activitiesIds.clear();
  }

}
