package org.exoplatform.datacollector;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.space.model.Space;

@RunWith(MockitoJUnitRunner.class)
public class UserInfluencersTest {
  
  UserInfluencers userInfluencers;
  
  @Before
  public void before() {
    Identity james = new Identity("james");
    Identity mary = new Identity("mary");
    Identity john = new Identity("john");
    Identity jack = new Identity("jack");
 
    List<Identity> connections = Arrays.asList(mary, john, jack);
    
    Space spaceMarketing = new Space();
    spaceMarketing.setId("marketing_team");
    spaceMarketing.setDisplayName("marketing_team");
    spaceMarketing.setManagers(new String[] {mary.getId()});
    spaceMarketing.setMembers(new String[] {mary.getId(), james.getId(), jack.getId()});
    
    Space spaceSupport = new Space();
    spaceSupport.setId("support_team");
    spaceSupport.setDisplayName("support_team");
    spaceSupport.setManagers(new String[] {john.getId()});
    spaceSupport.setMembers(new String[] {mary.getId(), john.getId(), james.getId()});
    
    List<Space> spaces = Arrays.asList(spaceMarketing, spaceSupport);
    
    userInfluencers = new UserInfluencers(james, connections, spaces);
    
    userInfluencers.addStream("Stream1", 0.2);
    userInfluencers.addStream("Stream2", 0.6);
    userInfluencers.addStream("Stream2", 0.3);
    userInfluencers.addStream("Stream4", 0.8);
    
    userInfluencers.addStream("Stream1", 0.4);
    userInfluencers.addStream("Stream1", 0.1);
  }
  
  @Test
  public void testGetStreamWeight() {
    Assert.assertEquals(0.32885, userInfluencers.getStreamWeight("Stream1"), 0.00001);
    Assert.assertEquals(0.44751, userInfluencers.getStreamWeight("Stream2"), 0.00001);
    Assert.assertEquals(0.44751, userInfluencers.getStreamWeight("Stream2"), 0.00001);
    Assert.assertEquals(UserInfluencers.DEFAULT_WEIGHT, userInfluencers.getStreamWeight("Stream22"), 0.00001);
  }

}
