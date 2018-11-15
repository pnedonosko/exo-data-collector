package org.exoplatform.datacollector.dao;

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

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-root-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/jcr/jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-portal-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-datacollector-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test-portal-configuration.xml") })
public class ActivityDAOTest extends BaseCommonsTestCase {

  /** Logger */
  private static final Log     LOG = ExoLogger.getExoLogger(ActivityDAOTest.class);

  private ActivityCommentedDAO activityCommentDAO;

  private SpaceService         spaceService;

  private RelationshipManager  relationshipManager;

  private IdentityManager      identityManager;

  private ActivityManager      activityManager;

  @Override
  protected void beforeClass() {
    super.beforeClass();
    PortalContainer container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);

    activityCommentDAO = (ActivityCommentedDAO) container.getComponentInstanceOfType(ActivityCommentedDAO.class);
    spaceService = (SpaceService) container.getComponentInstanceOfType(SpaceService.class);
    relationshipManager = (RelationshipManager) container.getComponentInstanceOfType(RelationshipManager.class);
    identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);
    activityManager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);

    initSpaces();
    initConnections();

    initActivities();
    // TODO: Check if the activity is already created
    // TODO: Create activity without a space
  }

  @Test
  public void testFindPartIsCommentedPoster() {
    assertTrue(activityCommentDAO.findPartIsCommentedPoster("john").isEmpty());
  }

  @Test
  public void testFindPartIsCommentedCommenter() {
    assertTrue(activityCommentDAO.findPartIsCommentedCommenter("john").isEmpty());
  }

  @Test
  public void testFindPartIsCommentedConvoPoster() {
    assertTrue(activityCommentDAO.findPartIsCommentedConvoPoster("john").isEmpty());
  }

  @Override
  protected void afterClass() {
    super.afterClass();

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
   * Creates space
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
      e.printStackTrace();
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
      e.printStackTrace();
    }
  }

  /**
   * Initializes connections between users
   */
  private void initConnections() {

    TestUtils.getConnections().forEach((invitingUser, invitedUsers) -> {
      Identity invitingIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, invitingUser, true);

      for (String invitedUser : invitedUsers) {
        Identity invitedIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, invitedUser, true);
        relationshipManager.inviteToConnect(invitingIdentity, invitedIdentity);
        relationshipManager.confirm(invitedIdentity, invitingIdentity);
      }

    });

  }

  private void initActivities() {
    JSONObject json = TestUtils.getJSON("activities.json");

    try {
      JSONArray activities = json.getJSONObject("data").getJSONArray("activities");

      for (int i = 0; i < activities.length(); i++) {
        RequestLifeCycle.begin(PortalContainer.getInstance());

        try {

          JSONObject activity = activities.getJSONObject(i);
          pushActivity(activity);

        } catch (Exception e) {
          LOG.error("Error when creating activity number" + i, e);
        } finally {
          RequestLifeCycle.end();
        }
      }

    } catch (JSONException e) {
      e.printStackTrace();
    }

  }

  /**
   * Push activity.
   *
   * @param activityJSON the activity JSON
   * @throws Exception the exception
   */
  private void pushActivity(JSONObject activityJSON) throws Exception {

    String from = activityJSON.getString("from");
    String space = activityJSON.getString("space");
    String body = activityJSON.getString("body");
    String title = activityJSON.getString("title");

    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, from, true);
    Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space, true);
    ExoSocialActivity activity =
                               new ExoSocialActivityImpl(userIdentity.getId(), SpaceActivityPublisher.SPACE_APP_ID, title, body);

    activityManager.saveActivityNoReturn(spaceIdentity, activity);
    activity.setPermanLink(LinkProvider.getSingleActivityUrl(activity.getId()));

    Thread.sleep(500);

    // Likes
    JSONArray likes = activityJSON.getJSONArray("likes");

    for (int i = 0; i < likes.length(); i++) {
      String like = likes.getString(i);
      Identity identityLike = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, like, false);
      try {
        activityManager.saveLike(activity, identityLike);
      } catch (Exception e) {
        LOG.error("Error when liking an activity with " + like, e);
      }
    }

    // Comments
    JSONArray comments = activityJSON.getJSONArray("comments");
    for (int i = 0; i < comments.length(); i++) {
      JSONObject commentJSON = comments.getJSONObject(i);

      Thread.sleep(1000);
      Identity identityComment = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                     commentJSON.getString("from"),
                                                                     false);
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle(commentJSON.getString("body"));
      comment.setUserId(identityComment.getId());
      activityManager.saveComment(activity, comment);
    }

  }

}
