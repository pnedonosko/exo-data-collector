package org.exoplatform.datacollector.dao;

import java.util.List;

import org.junit.Test;

import org.exoplatform.datacollector.domain.ActivityCommentedEntity;

public class ActivityCommentedDAOTest extends AbstractActivityDAOTest {

  private ActivityCommentedDAO activityCommentedDAO;

  @Override
  protected void beforeClass() {
    super.beforeClass();
    activityCommentedDAO = (ActivityCommentedDAO) container.getComponentInstance(ActivityCommentedDAO.class);
  }

  @Test
  public void testFindPartIsCommentedPoster() {
    List<ActivityCommentedEntity> res = activityCommentedDAO.findPartIsCommentedPoster(johnId.getId());
    assertEquals(1, res.size());
    assertEquals(maryId.getId(), res.get(0).getPosterId());
  }

  @Test
  public void testFindPartIsCommentedCommenter() {
    assertTrue(activityCommentedDAO.findPartIsCommentedCommenter(johnId.getId()).isEmpty());
  }

  @Test
  public void testFindPartIsCommentedConvoPoster() {
    assertTrue(activityCommentedDAO.findPartIsCommentedConvoPoster(johnId.getId()).isEmpty());
  }

}
