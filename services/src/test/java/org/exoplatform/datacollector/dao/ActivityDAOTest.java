package org.exoplatform.datacollector.dao;

import org.junit.Test;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.security.IdentityRegistry;
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

  @Override
  protected void beforeClass() {
    super.beforeClass();
    PortalContainer container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);

    activityCommentDAO = (ActivityCommentedDAO) container.getComponentInstanceOfType(ActivityCommentedDAO.class);
    SpaceService spaceService = (SpaceService) container.getComponentInstanceOfType(SpaceService.class);
    
    Space space = new Space();
    
    String[] managers = new String[] { "raul" };
    String[] members = new String[] { "raul" };
    

    space.setDisplayName("testingSpace");
    space.setDescription("Space for testing");
    space.setPrettyName("testingSpace");
    space.setGroupId("/spaces/space_test"); 
    space.setVisibility(Space.PUBLIC);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setType("classic");
    space.setManagers(managers);
    space.setMembers(members);

    spaceService.createSpace(space, "raul");

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

}
