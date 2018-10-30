package org.exoplatform.datacollector;

import org.exoplatform.datacollector.domain.RelevanceEntity;
import org.exoplatform.datacollector.domain.RelevanceId;

public class TestUtils {

  public static RelevanceId EXISTING_RELEVANCE_ID   = new RelevanceId("1", "2");

  public static RelevanceId UNEXISTING_RELEVANCE_ID = new RelevanceId("1", "3");

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
