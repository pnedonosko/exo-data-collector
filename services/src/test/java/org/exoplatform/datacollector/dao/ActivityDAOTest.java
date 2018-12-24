package org.exoplatform.datacollector.dao;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.datacollector.BaseActivityTestCase;
import org.exoplatform.datacollector.domain.ActivityCommentedEntity;
import org.exoplatform.datacollector.domain.ActivityLikedEntity;
import org.exoplatform.datacollector.domain.ActivityMentionedEntity;
import org.exoplatform.datacollector.domain.ActivityPostedEntity;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ActivityStream.Type;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class ActivityDAOTest extends BaseActivityTestCase {

  /** Logger */
  private static final Log     LOG = ExoLogger.getExoLogger(ActivityDAOTest.class);

  private ActivityCommentedDAO activityCommentDAO;

  private ActivityPostedDAO    activityPostedDAO;

  private ActivityMentionedDAO activityMentionedDAO;

  private ActivityLikedDAO     activityLikedDAO;
  
  @Override
  protected void beforeClass() {
    super.beforeClass();
    
    activityCommentDAO = getService(ActivityCommentedDAO.class);
    activityPostedDAO = getService(ActivityPostedDAO.class);
    activityMentionedDAO = getService(ActivityMentionedDAO.class);
    activityLikedDAO = getService(ActivityLikedDAO.class);
  }

  // ActivityCommentedDAO tests
  @Test
  public void testFindPartIsCommentedPoster() {
    List<ActivityCommentedEntity> res = activityCommentDAO.findPartIsCommentedPoster(jasonId.toString());
    assertEquals(3, res.size());

    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(jackId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(bobId)).count());

    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(engineeringId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  @Test
  public void testFindPartIsCommentedCommenter() {
    List<ActivityCommentedEntity> res = activityCommentDAO.findPartIsCommentedCommenter(jasonId);
    assertEquals(5, res.size());

    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(bobId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(aliceId)).count());

    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
  }

  @Test
  public void testFindPartIsCommentedConvoPoster() {
    List<ActivityCommentedEntity> res = activityCommentDAO.findPartIsCommentedConvoPoster(jasonId);
    assertEquals(2, res.size());

    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());

    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
  }

  @Test
  public void testFindPartIsPostCommenter() {
    List<ActivityCommentedEntity> res = activityCommentDAO.findPartIsPostCommenter(jasonId);
    assertEquals(4, res.size());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());

    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  @Test
  public void testFindPartIsCommentCommenter() {
    List<ActivityCommentedEntity> res = activityCommentDAO.findPartIsCommentCommenter(jasonId);
    assertEquals(5, res.size());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(jackId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(jamesId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());

    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());

    assertEquals(1, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  @Test
  public void testFindPartIsConvoCommenter() {
    List<ActivityCommentedEntity> res = activityCommentDAO.findPartIsConvoCommenter(jasonId);
    assertEquals(2, res.size());

    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(jamesId)).count());

    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
  }

  @Test
  public void testFindPartIsFavoriteStreamCommenter() {
    List<String> favoriteStreams = Arrays.asList(productId, engineeringId, marketingId, supportId, salesId, bobId);
    List<ActivityCommentedEntity> res = activityCommentDAO.findPartIsFavoriteStreamCommenter(jasonId, favoriteStreams);
    assertEquals(23, res.size());

    assertEquals(4, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(jackId)).count());
    assertEquals(5, res.stream().filter(entity -> entity.getPosterId().equals(jamesId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(bobId)).count());
    assertEquals(5, res.stream().filter(entity -> entity.getPosterId().equals(aliceId)).count());

    assertEquals(6, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(10, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  // ActivityPostedDAO tests

  @Test
  public void testFindUserPosts() {
    List<ActivityPostedEntity> res = activityPostedDAO.findUserPosts(jasonId.toString());
    assertEquals(4, res.size());
    assertEquals(1,
                 res.stream()
                    .filter(entity -> entity.getOwnerId().equals(marketingId)
                        && entity.getProviderId().equals(Type.SPACE.toString()))
                    .count());
    assertEquals(2,
                 res.stream()
                    .filter(entity -> entity.getOwnerId().equals(productId)
                        && entity.getProviderId().equals(Type.SPACE.toString()))
                    .count());
    assertEquals(1, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  @Test
  public void testFindPartIsFavoriteStreamPoster() {
    List<String> favoriteStreams = Arrays.asList(productId, engineeringId, marketingId, supportId, salesId, bobId);
    List<ActivityPostedEntity> res = activityPostedDAO.findPartIsFavoriteStreamPoster(jasonId, favoriteStreams);
    assertEquals(8, res.size());

    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(jackId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(jamesId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(aliceId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(bobId)).count());

    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(engineeringId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());

  }

  // ActivityMentionedDAO tests
  
  @Test
  public void testfindPartIsMentioned() {
    List<ActivityMentionedEntity> res = activityMentionedDAO.findPartIsMentioned(jasonId);
    assertEquals(11, res.size());

    assertTrue(res.stream().allMatch(entity -> entity.getPosterId().equals(jasonId)));

    assertEquals(3, res.stream().filter(entity -> entity.getMentionedId().equals(maryId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getMentionedId().equals(jamesId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getMentionedId().equals(jackId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getMentionedId().equals(johnId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getMentionedId().equals(bobId)).count());
  }

  @Test
  public void testFindPartIsMentioner() {
    List<ActivityMentionedEntity> res = activityMentionedDAO.findPartIsMentioner(jasonId.toString());
    assertEquals(8, res.size());
    assertTrue(res.stream().allMatch(entity -> entity.getMentionedId().equals(jasonId.toString())));

    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(jamesId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(jackId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(aliceId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(bobId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());

    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(johnId)).count());
  }

  // ActivityLikedEntity tests

  @Test
  public void testFindPartIsLikedPoster() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsLikedPoster(jasonId.toString());
    assertEquals(6, res.size());

    assertTrue(res.stream().allMatch(entity -> entity.getLikerId().equals(jasonId.toString())));

    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(jackId)).count());

    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(engineeringId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  @Test
  public void testFindPartIsLikedCommenter() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsLikedCommenter(jasonId.toString());
    assertEquals(14, res.size());

    assertTrue(res.stream().allMatch(entity -> entity.getLikerId().equals(jasonId.toString())));

    assertEquals(4, res.stream().filter(entity -> entity.getPosterId().equals(jamesId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(bobId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(jackId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(aliceId)).count());

    assertEquals(4, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(4, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  @Test
  public void testFindPartIsLikedConvoPoster() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsLikedConvoPoster(jasonId.toString());
    assertEquals(5, res.size());
    assertTrue(res.stream().allMatch(entity -> entity.getLikerId().equals(jasonId.toString())));

    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(jamesId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(aliceId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(bobId)).count());
    
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(bobId)).count());
    
  }

  @Test
  public void testFindPartIsPostLiker() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsPostLiker(jasonId.toString());
    assertEquals(11, res.size());

    assertTrue(res.stream().allMatch(entity -> entity.getPosterId().equals(jasonId.toString())));

    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(jamesId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(maryId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(johnId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getLikerId().equals(jackId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getLikerId().equals(bobId)).count());

    assertEquals(7, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  @Test
  public void testFindPartIsCommentLiker() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsCommentLiker(jasonId.toString());
    assertEquals(17, res.size());

    assertTrue(res.stream().allMatch(entity -> entity.getPosterId().equals(jasonId.toString())));

    assertEquals(4, res.stream().filter(entity -> entity.getLikerId().equals(jamesId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(johnId)).count());
    assertEquals(4, res.stream().filter(entity -> entity.getLikerId().equals(jackId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(maryId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(aliceId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getLikerId().equals(bobId)).count());

    assertEquals(7, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(engineeringId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());

  }

  @Test
  public void testFindPartIsConvoLiker() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsConvoLiker(jasonId);
    assertEquals(14, res.size());

    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(johnId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(jackId)).count());
    assertEquals(4, res.stream().filter(entity -> entity.getLikerId().equals(jamesId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(maryId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(bobId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(aliceId)).count());

    assertEquals(4, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(engineeringId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  @Test
  public void testFindPartIsSamePostLiker() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsSamePostLiker(jasonId);
    assertEquals(9, res.size());

    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(jackId)).count());

    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(jamesId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(bobId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(aliceId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getLikerId().equals(maryId)).count());

    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(engineeringId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(4, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  @Test
  public void testFindPartIsSameCommentLiker() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsSameCommentLiker(jasonId);
    assertEquals(12, res.size());

    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(jackId)).count());
    assertEquals(4, res.stream().filter(entity -> entity.getLikerId().equals(maryId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(jamesId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getLikerId().equals(johnId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getLikerId().equals(aliceId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(bobId)).count());

    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    assertEquals(4, res.stream().filter(entity -> entity.getPosterId().equals(jamesId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(aliceId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(bobId)).count());

    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(5, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  
  @Test
  public void testFindPartIsSameConvoLiker() {
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsSameConvoLiker(jasonId);
    assertEquals(7, res.size());
   
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(johnId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(aliceId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getLikerId().equals(bobId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getLikerId().equals(jackId)).count());
    
    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(jamesId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(aliceId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(bobId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    
    assertEquals(4, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }
  
  
  @Test
  public void testFindPartIsFavoriteStreamPostLiker() {
    List<String> favoriteStreams = Arrays.asList(productId, engineeringId, marketingId, supportId, salesId, bobId);
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsFavoriteStreamPostLiker(jasonId, favoriteStreams);
    assertEquals(21, res.size());

    assertEquals(4, res.stream().filter(entity -> entity.getLikerId().equals(johnId)).count());
    assertEquals(5, res.stream().filter(entity -> entity.getLikerId().equals(jamesId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(maryId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(jackId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(bobId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(aliceId)).count());
    
    assertEquals(9, res.stream().filter(entity -> entity.getPosterId().equals(jasonId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(johnId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(maryId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(jackId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getPosterId().equals(bobId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getPosterId().equals(aliceId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getPosterId().equals(jamesId)).count());
    
    assertEquals(7, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(engineeringId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(7, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(1, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getProviderId().equals(Type.USER.toString())).count());
  }

  
  @Test
  public void testFindPartIsFavoriteStreamCommentLiker() {
    List<String> favoriteStreams = Arrays.asList(productId, engineeringId, marketingId, supportId, salesId, bobId);
    List<ActivityLikedEntity> res = activityLikedDAO.findPartIsFavoriteStreamCommentLiker(jasonId, favoriteStreams);
    assertEquals(39, res.size());
    
    assertEquals(4, res.stream().filter(entity -> entity.getLikerId().equals(johnId)).count());
    assertEquals(5, res.stream().filter(entity -> entity.getLikerId().equals(aliceId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getLikerId().equals(bobId)).count());
    assertEquals(9, res.stream().filter(entity -> entity.getLikerId().equals(jackId)).count());
    assertEquals(8, res.stream().filter(entity -> entity.getLikerId().equals(jamesId)).count());
    assertEquals(10, res.stream().filter(entity -> entity.getLikerId().equals(maryId)).count());
    
    assertEquals(11, res.stream().filter(entity -> entity.getOwnerId().equals(productId)).count());
    assertEquals(2, res.stream().filter(entity -> entity.getOwnerId().equals(engineeringId)).count());
    assertEquals(5, res.stream().filter(entity -> entity.getOwnerId().equals(marketingId)).count());
    assertEquals(14, res.stream().filter(entity -> entity.getOwnerId().equals(supportId)).count());
    assertEquals(4, res.stream().filter(entity -> entity.getOwnerId().equals(salesId)).count());
    assertEquals(3, res.stream().filter(entity -> entity.getOwnerId().equals(bobId)).count());
  }
  
}
