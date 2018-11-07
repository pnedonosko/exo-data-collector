package org.exoplatform.datacollector;

import org.exoplatform.datacollector.domain.RelevanceEntity;
import org.exoplatform.datacollector.domain.RelevanceId;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

public class TestUtils {

  public static RelevanceId    EXISTING_RELEVANCE_ID   = new RelevanceId("1", "2");

  public static RelevanceId    UNEXISTING_RELEVANCE_ID = new RelevanceId("1", "3");

  public static final String   SUPPORT_TEAM            = "Support Team";

  public static final String   SALES_TEAM              = "Sales Team";

  public static final String   PRODUCT_TEAM            = "Product Team";

  public static final String   MARKETING_TEAM          = "Marketing Team";

  public static final String   ENGINEERING_TEAM        = "Engineering Team";

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
}
