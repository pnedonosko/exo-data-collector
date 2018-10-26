package org.exoplatform.datacollector.dao;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-root-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/jcr/jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path =  "conf/standalone/test-portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test-portal-configuration.xml"
  )
})
public class ActivityPostDAOTest extends BaseCommonsTestCase {

  private ActivityPostDAO activityPostDAO;

  @Before
  public void initDAO() {
    PortalContainer container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    activityPostDAO = (ActivityPostDAO) container.getComponentInstanceOfType(ActivityPostDAO.class);
  }

  @Test
  public void testFindPartIsCommentedPoster() {
    assertTrue(activityPostDAO.findPartIsCommentedPoster("john").isEmpty());
  }

  @Test
  public void testFindPartIsCommentedCommenter() {
    assertTrue(activityPostDAO.findPartIsCommentedCommenter("john").isEmpty());
  }

  @Test
  public void testFindPartIsCommentedConvoPoster() {
    assertTrue(activityPostDAO.findPartIsCommentedConvoPoster("john").isEmpty());
  }
}
