package org.exoplatform.datacollector.dao;

import java.util.Map;

import org.junit.Test;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.datacollector.TestUtils;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
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

  private ActivityCommentedDAO activityCommentDAO;

  private SpaceService         spaceService;

  private RelationshipManager  relationshipManager;

  private IdentityManager      identityManager;

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

    initSpaces();
    initConnections();

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
   * Initialized connections between users
   */
  private void initConnections() {

    Map<String, String[]> connections = TestUtils.getConnections();

    connections.forEach((invitingUser, invitedUsers) -> {
      Identity invitingIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, invitingUser, true);

      for (String invitedUser : invitedUsers) {
        Identity invitedIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, invitedUser, true);
        relationshipManager.inviteToConnect(invitingIdentity, invitedIdentity);
        relationshipManager.confirm(invitedIdentity, invitingIdentity);
      }

    });

  }

}
