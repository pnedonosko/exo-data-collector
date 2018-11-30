package org.exoplatform.datacollector.dao;

import java.util.List;

import org.junit.Test;

import org.exoplatform.datacollector.domain.ActivityMentionedEntity;

public class ActivityMentionedDAOTest extends AbstractActivityDAOTest {

  private ActivityMentionedDAO activityMentionedDAO;

  @Override
  protected void beforeClass() {
    super.beforeClass();
    activityMentionedDAO = (ActivityMentionedDAO) container.getComponentInstance(ActivityMentionedDAO.class);
  }

  /*
  @Test
  public void testfindPartIsMentioned() {
    List<ActivityMentionedEntity> res = activityMentionedDAO.findPartIsMentioned(jasonId.getId());
    assertEquals(2, res.size());
    assertTrue(res.stream().allMatch(entity -> entity.getPosterId().equals(jasonId.getId())));
  }
  */

  @Test
  public void testFindPartIsMentioner() {
    List<ActivityMentionedEntity> res = activityMentionedDAO.findPartIsMentioner(jamesId.getId());
    assertEquals(2, res.size());
    assertTrue(res.stream().allMatch(entity -> entity.getMentionedId().equals(jamesId.getId())));
  }

}
