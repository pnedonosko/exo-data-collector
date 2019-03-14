package org.exoplatform.datacollector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
public class SocialInfluencersTest {

  static SocialInfluencers  jamesInfluencers;

  private static final Long MILISECONDS_IN_DAY = 86400000L;

  @BeforeClass
  public static void before() {
    Identity james = new Identity("james");
    Identity mary = new Identity("mary");
    Identity john = new Identity("john");
    Identity jack = new Identity("jack");
    Identity alice = new Identity("alice");
    Set<String> connections = new LinkedHashSet<>(Arrays.asList(mary.getId(), john.getId(), jack.getId()));

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

    jamesInfluencers =
                     new SocialInfluencers(connections,
                                           Collections.unmodifiableMap(spaces.stream()
                                                                             .collect(Collectors.toMap(s -> s.getId(),
                                                                                                       s -> new SpaceSnapshot(s),
                                                                                                       (s1, s2) -> s1))));

    jamesInfluencers.addStream("Stream1", 0.2);
    jamesInfluencers.addStream("Stream1", 0.4);
    jamesInfluencers.addStream("Stream1", 0.1); // influencer #3
    jamesInfluencers.addStream("Stream2", 0.6);
    jamesInfluencers.addStream("Stream2", 0.3); // influencer #1
    jamesInfluencers.addStream("Stream3", 0.8); // influencer #2

    jamesInfluencers.addParticipant("mary", 0.2); // "mary"
    jamesInfluencers.addParticipant("mary", 0.1);
    jamesInfluencers.addParticipant("james", 0.8); // "james"
    jamesInfluencers.addParticipant("jack", 0.3); // "jack"
    jamesInfluencers.addParticipant("jack", 0.3);
    jamesInfluencers.addParticipant("jack", 0.1);
    jamesInfluencers.addParticipant("john", 0.3); // "john"

    jamesInfluencers.addPost(new ActivityPostedEntityMock("mary", 10000L));
    jamesInfluencers.addPost(new ActivityPostedEntityMock("james", 450000L));

    jamesInfluencers.addComment(new ActivityCommentedEntityMock("mary", 10000L, MILISECONDS_IN_DAY + 10000L));
    jamesInfluencers.addComment(new ActivityCommentedEntityMock("james", 450000L, 94000L));
  }

  @Test
  public void testGetStreamWeight() {
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(0.7))), jamesInfluencers.getStreamWeight("Stream1"), 0.00001);
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(0.9))), jamesInfluencers.getStreamWeight("Stream2"), 0.00001);
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(0.8))), jamesInfluencers.getStreamWeight("Stream3"), 0.00001);
    Assert.assertEquals(SocialInfluencers.DEFAULT_WEIGHT, jamesInfluencers.getStreamWeight("Stream22"), 0.00001);
  }

  @Test
  public void testGetFavoriteStreams() {
    Collection<String> result = jamesInfluencers.getFavoriteStreams();
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals("Stream2", result.toArray()[0]);
    Assert.assertEquals("Stream3", result.toArray()[1]);
    Assert.assertEquals("Stream1", result.toArray()[2]);
  }

  @Test
  public void testGetFavoriteStreamsTop() {
    Collection<String> result = jamesInfluencers.getFavoriteStreamsTop(2);
    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals("Stream2", result.toArray()[0]);
    Assert.assertEquals("Stream3", result.toArray()[1]);
  }

  @Test
  public void testGetParticipantWeight() {
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(1.1))),
                        jamesInfluencers.getParticipantWeight("mary", "marketing_team"),
                        0.00001);
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(1.1))),
                        jamesInfluencers.getParticipantWeight("jack", "support_team"),
                        0.00001);
    Assert.assertEquals(1 / (1 + Math.exp(-2 * Math.log(0.1))),
                        jamesInfluencers.getParticipantWeight("alice", "support_team"),
                        0.00001);
  }

  @Test
  public void testGetPostReactivity() {
    // TODO: find out with getPostReactivity
    System.out.println(jamesInfluencers.getPostReactivity("post1"));
    System.out.println(jamesInfluencers.getPostReactivity("post2"));
  }
}
