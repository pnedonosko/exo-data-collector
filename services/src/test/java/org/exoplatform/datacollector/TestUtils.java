package org.exoplatform.datacollector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.datacollector.domain.RelevanceEntity;
import org.exoplatform.datacollector.domain.RelevanceId;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;

public class TestUtils {

  public static RelevanceId    EXISTING_RELEVANCE_ID   = new RelevanceId("1", "2");

  public static RelevanceId    UNEXISTING_RELEVANCE_ID = new RelevanceId("1", "3");

  public static final String   SUPPORT_TEAM            = "Support Team";

  public static final String   SALES_TEAM              = "Sales Team";

  public static final String   PRODUCT_TEAM            = "Product Team";

  public static final String   MARKETING_TEAM          = "Marketing Team";

  public static final String   ENGINEERING_TEAM        = "Engineering Team";

  public static final String[] ENGINERING_MANAGERS     = new String[] { "john", "bob", "jason" };

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

  /**
   * Gets a map with testing connections between users.
   * The key is an username who invites to connect
   * The value is an array with usernames which are invited 
   * @return map with connections
   */
  public static Map<String, String[]> getConnections() {
    Map<String, String[]> connections = new HashMap<>();
    connections.put("alice", new String[] { "bob", "james", "john", "mary", "jason" });
    connections.put("bob", new String[] { "alice", "jack", "james", "john", "mary", "jason" });
    connections.put("jack", new String[] { "bob", "james", "john", "jason" });
    connections.put("james", new String[] { "alice", "bob", "jack", "john", "mary", "jason" });
    connections.put("john", new String[] { "alice", "bob", "jack", "james", "jason" });
    connections.put("mary", new String[] { "alice", "james", "john", "jason" });
    connections.put("jason", new String[] { "alice", "bob", "jack", "james", "john", "mary" });
    return connections;
  }

  public static JSONObject getJSON(String fileName) {

    InputStream stream = null;
    stream = TestUtils.class.getClassLoader().getResourceAsStream("scenarios/" + fileName);

    String fileContent = getData(stream);
    JSONObject json = null;
    try {
      json = new JSONObject(fileContent);
    } catch (JSONException e) {

      e.printStackTrace();
    }

    return json;
  }

  /**
   * Gets the data.
   *
   * @param inputStream the input stream
   * @return the data
   */
  public static String getData(InputStream inputStream) {
    String out = "";
    StringWriter writer = new StringWriter();
    try {
      IOUtils.copy(inputStream, writer);
      out = writer.toString();

    } catch (IOException e) {
      e.printStackTrace();
    }

    return out;
  }


}
