package org.exoplatform.datacollector;

import org.exoplatform.datacollector.domain.RelevanceEntity;
import org.exoplatform.datacollector.domain.RelevanceId;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

public class TestUtils {

  public static RelevanceId    EXISTING_RELEVANCE_ID   = new RelevanceId("1", "2");

  public static RelevanceId    UNEXISTING_RELEVANCE_ID = new RelevanceId("1", "3");

  private static final String  SUPPORT_TEAM            = "Support Team";

  private static final String  SALES_TEAM              = "Sales Team";

  private static final String  PRODUCT_TEAM            = "Product Team";

  private static final String  MARKETING_TEAM          = "Marketing Team";

  private static final String  ENGINEERING_TEAM        = "Engineering Team";

  public static final String[] ENGINERING_MANAGERS     = new String[] { "john", "bob" };

  public static final String[] ENGINERING_MEMBERS      = new String[] { "james", "bob", "john", "jack" };

  public static final String[] MARKETING_MANAGERS      = new String[] { "mary" };

  public static final String[] MARKETING_MEMBERS       = new String[] { "mary", "bob", "jason" };

  public static final String[] PRODUCT_MANAGERS        = new String[] { "james", "jason" };

  public static final String[] PRODUCT_MEMBERS         = new String[] { "john", "jason", "jack", "james" };

  public static final String[] SALES_MANAGERS          = new String[] { "jason", "mary" };

  public static final String[] SALES_MEMBERS           = new String[] { "jason", "mary", "peter" };

  public static final String[] SUPPORT_MANAGERS        = new String[] { "alice", "james" };

  public static final String[] SUPPORT_MEMBERS         =
                                               new String[] { "alice", "james", "john", "jack", "bob", "jason", "mary" };

  public static RelevanceEntity getExistingRelevance() {
    RelevanceEntity relevance = new RelevanceEntity();
    relevance.setUserId("1");
    relevance.setActivityId("2");
    relevance.setRelevant(true);
    return relevance;
  }

  public static RelevanceEntity getNewRelevance() {
    RelevanceEntity relevance = new RelevanceEntity();
    relevance.setUserId("1");
    relevance.setActivityId("3");
    relevance.setRelevant(true);
    return relevance;
  }

  public static void createSpace(SpaceService spaceService, String displayName, String[] managers, String[] members) {
    Space space = new Space();
    space.setDisplayName(displayName);
    space.setPrettyName(displayName);
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space" + displayName);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.PUBLIC);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    String groupId = null;
    try {
      groupId = SpaceUtils.createGroup(space.getDisplayName(), space.getPrettyName(), managers[0]);
    } catch (SpaceException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    space.setGroupId(groupId);
    space.setUrl(space.getPrettyName());
    String[] invitedUsers = new String[] {};
    String[] pendingUsers = new String[] {};
    space.setInvitedUsers(invitedUsers);
    space.setPendingUsers(pendingUsers);
    space.setManagers(managers);
    space.setMembers(members);
    try {
      spaceService.saveSpace(space, true);
    } catch (SpaceException e) {
      e.printStackTrace();
    }
  }

  public static void initSpaces(SpaceService spaceService) {
    if (spaceService.getSpaceByDisplayName(ENGINEERING_TEAM) == null) {
      createSpace(spaceService, ENGINEERING_TEAM, ENGINERING_MANAGERS, ENGINERING_MEMBERS);
    }
    if (spaceService.getSpaceByDisplayName(MARKETING_TEAM) == null) {
      createSpace(spaceService, MARKETING_TEAM, MARKETING_MANAGERS, MARKETING_MEMBERS);
    }
    if (spaceService.getSpaceByDisplayName(PRODUCT_TEAM) == null) {
      createSpace(spaceService, PRODUCT_TEAM, PRODUCT_MANAGERS, PRODUCT_MEMBERS);
    }
    if (spaceService.getSpaceByDisplayName(SALES_TEAM) == null) {
      createSpace(spaceService, SALES_TEAM, SALES_MANAGERS, SALES_MEMBERS);
    }
    if (spaceService.getSpaceByDisplayName(SUPPORT_TEAM) == null) {
      createSpace(spaceService, SUPPORT_TEAM, SUPPORT_MANAGERS, SUPPORT_MEMBERS);
    }
  }
}
