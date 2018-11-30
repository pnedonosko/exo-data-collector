package org.exoplatform.datacollector.dao;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.exoplatform.datacollector.domain.ActivityPostedEntity;
import org.exoplatform.social.core.activity.model.ActivityStream.Type;

public class ActivityPostedDAOTest extends AbstractActivityDAOTest {

  private ActivityPostedDAO activityPostedDAO;

  @Override
  protected void beforeClass() {
    super.beforeClass();
    activityPostedDAO = (ActivityPostedDAO) container.getComponentInstance(ActivityPostedDAO.class);
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
    List<ActivityPostedEntity> res = activityPostedDAO.findPartIsFavoriteStreamPoster(johnId.getId(),
                                                                                      Arrays.asList(supportId.getId(),
                                                                                                    marketingId.getId()));
    assertEquals(2, res.size());
    assertTrue(res.stream().allMatch(entity -> !entity.getPosterId().equals(johnId.getId())));
  }

}
