package org.exoplatform.datacollector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.datacollector.domain.ActivityCommentedEntityMock;
import org.exoplatform.datacollector.domain.ActivityPostedEntityMock;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.space.model.Space;

@RunWith(MockitoJUnitRunner.class)
public class UserInfluencersTest {

  static UserInfluencers    userInfluencers;

  private static final Long MILISECONDS_IN_DAY = 86400000L;

  @BeforeClass
  public static void before() {
    Identity james = new Identity("james");
    Identity mary = new Identity("mary");
    Identity john = new Identity("john");
    Identity jack = new Identity("jack");
    Identity alice = new Identity("alice");
    List<Identity> connections = Arrays.asList(mary, john, jack);

    Space spaceMarketing = new Space();
    spaceMarketing.setId("marketing_team");
    spaceMarketing.setDisplayName("marketing_team");
    spaceMarketing.setManagers(new String[] { mary.getId() });
    spaceMarketing.setMembers(new String[] { mary.getId(), james.getId(), jack.getId() });

    Space spaceSupport = new Space();
    spaceSupport.setId("support_team");
    spaceSupport.setDisplayName("support_team");
    spaceSupport.setManagers(new String[] { john.getId() });
    spaceSupport.setMembers(new String[] { mary.getId(), john.getId(), james.getId() });

    List<Space> spaces = Arrays.asList(spaceMarketing, spaceSupport);

    userInfluencers = new UserInfluencers(james, connections, spaces);

    userInfluencers.addStream("Stream1", 0.2);
    userInfluencers.addStream("Stream1", 0.4);
    userInfluencers.addStream("Stream1", 0.1); // influencer #3
    userInfluencers.addStream("Stream2", 0.6);
    userInfluencers.addStream("Stream2", 0.3); // influencer #1
    userInfluencers.addStream("Stream3", 0.8); // influencer #2

    userInfluencers.addParticipant("mary", 0.2); // "mary"
    userInfluencers.addParticipant("mary", 0.1);
    userInfluencers.addParticipant("james", 0.8); // "james"
    userInfluencers.addParticipant("jack", 0.3); // "jack"
    userInfluencers.addParticipant("jack", 0.3);
    userInfluencers.addParticipant("jack", 0.1);
    userInfluencers.addParticipant("john", 0.3); // "john"

    userInfluencers.addPost(new ActivityPostedEntityMock("mary", 10000L));
    userInfluencers.addPost(new ActivityPostedEntityMock("james", 450000L));

    userInfluencers.addComment(new ActivityCommentedEntityMock("mary", 10000L, MILISECONDS_IN_DAY + 10000L));
    userInfluencers.addComment(new ActivityCommentedEntityMock("james", 450000L, 94000L));
  }

  @Test
  public void testGetStreamWeight() {
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(0.7))), userInfluencers.getStreamWeight("Stream1"), 0.00001);
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(0.9))), userInfluencers.getStreamWeight("Stream2"), 0.00001);
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(0.8))), userInfluencers.getStreamWeight("Stream3"), 0.00001);
    Assert.assertEquals(UserInfluencers.DEFAULT_WEIGHT, userInfluencers.getStreamWeight("Stream22"), 0.00001);
  }

  @Test
  public void testGetFavoriteStreams() {
    Collection<String> result = userInfluencers.getFavoriteStreams();
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals("Stream2", result.toArray()[0]);
    Assert.assertEquals("Stream3", result.toArray()[1]);
    Assert.assertEquals("Stream1", result.toArray()[2]);
  }

  @Test
  public void testGetFavoriteStreamsTop() {
    Collection<String> result = userInfluencers.getFavoriteStreamsTop(2);
    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals("Stream2", result.toArray()[0]);
    Assert.assertEquals("Stream3", result.toArray()[1]);
  }

  @Test
  public void testGetParticipantWeight() {
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(1.1))),
                        userInfluencers.getParticipantWeight("mary", "marketing_team"),
                        0.00001);
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(1.1))),
                        userInfluencers.getParticipantWeight("jack", "support_team"),
                        0.00001);
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(0.1))),
                        userInfluencers.getParticipantWeight("alice", "support_team"),
                        0.00001);
  }

  @Test
  public void testGetPostReactivity() {
    // TODO: find out with getPostReactivity
    System.out.println(userInfluencers.getPostReactivity("post1"));
    System.out.println(userInfluencers.getPostReactivity("post2"));
  }
}
