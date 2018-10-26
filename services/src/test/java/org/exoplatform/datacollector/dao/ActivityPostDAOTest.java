package org.exoplatform.datacollector.dao;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.datacollector.AbstractTest;

public class ActivityPostDAOTest extends AbstractTest {
  
  private ActivityPostDAO activityPostDAO;
  
  @Before
  public void initDAO() {
    PortalContainer container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    activityPostDAO = (ActivityPostDAO) container.getComponentInstanceOfType(ActivityPostDAO.class);
  }
  
  @Test
  public void findPartIsCommentedPoster() {
    activityPostDAO.findPartIsCommentedPoster("john");
  }
  
  /*
  @Test
  public void testFindPartIsCommentedPoster() {
    activityPostDAO.findPartIsCommentedPoster("111");
  }
  */
  /*
  @Test
  public void testFindPartIsCommentedConvoPoster() {
    activityPostDAO.findPartIsCommentedConvoPoster("111");
  }*/

}
