package org.exoplatform.datacollector.dao;

import java.util.List;

import org.junit.Test;

import org.exoplatform.datacollector.domain.ActivityLikedEntity;

public class ActivityLikedDAOTest extends AbstractActivityDAOTest {

  private ActivityLikedDAO activityLikedDAO;

  @Override
  protected void beforeClass() {
    super.beforeClass();
    activityLikedDAO = (ActivityLikedDAO) container.getComponentInstance(ActivityLikedDAO.class);
  }

  @Test
  public void testFindPartIsLikedPoster() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsLikedPoster(johnId.getId());
    assertEquals(3, res.size());
    assertTrue(res.stream().allMatch(entity -> entity.getLikerId().equals(johnId.getId())));
  }

  @Test
  public void testFindPartIsLikedCommenter() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsLikedCommenter(johnId.getId());
    assertEquals(2, res.size());
    assertTrue(res.stream().allMatch(entity -> entity.getLikerId().equals(johnId.getId())));
  }

}
