package org.exoplatform.datacollector;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.injection.core.module.ActivityModule;
import org.exoplatform.injection.core.module.SpaceModule;
import org.exoplatform.injection.core.module.UserModule;
import org.exoplatform.injection.helper.InjectorMonitor;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class DataPopulator {
  /** The log. */
  private final Log               LOG                            = ExoLogger.getLogger(DataPopulator.class);

  /** The scenario folder. */
  public String                   SCENARIO_FOLDER                = "/scenarios";

  /** The scenario name attribute. */
  public String                   SCENARIO_NAME_ATTRIBUTE        = "scenarioName";

  /** The scenario description attribute. */
  public String                   SCENARIO_DESCRIPTION_ATTRIBUTE = "scenarioName";

  /** The scenarios. */
  private Map<String, JSONObject> scenarios;

  /** The users. */
  public final String             USERS                          = "Users";

  /** The spaces. */
  public final String             SPACES                         = "Spaces";

  /** The activities. */
  public final String             ACTIVITIES                     = "Activities";

  private ActivityModule                  activityModule;

  private SpaceModule                     spaceModule;

  private UserModule                      userModule;

  /**
   * Instantiates a new populator service.
   */
  public DataPopulator(ActivityModule activityModule, SpaceModule spaceModule, UserModule userModule) {
    this.activityModule = activityModule;
    this.spaceModule = spaceModule;
    this.userModule = userModule;

  }

  /**
   * Populate.
   *
   * @param scenarioName the scenario name
   * @return the string
   */
  public void populate(String scenarioName) {

    InjectorMonitor injectorMonitor = new InjectorMonitor("Data Injection Process");
    try {
      JSONObject scenarioData = scenarios.get(scenarioName).getJSONObject("data");
      if (scenarioData.has("users")) {
        LOG.info("Create " + scenarioData.getJSONArray("users").length() + " users.");
        injectorMonitor.start("Processing users data");
        userModule.createUsers(scenarioData.getJSONArray("users"), "injector-dataset");
        injectorMonitor.stop();

      }
      if (scenarioData.has("relations")) {
        LOG.info("Create " + scenarioData.getJSONArray("relations").length() + " relations.");
        injectorMonitor.start("Processing users data");
        userModule.createRelations(scenarioData.getJSONArray("relations"));
        injectorMonitor.stop();
      }
      if (scenarioData.has("spaces")) {
        LOG.info("Create " + scenarioData.getJSONArray("spaces").length() + " spaces.");

        injectorMonitor.start("Processing users data");
        spaceModule.createSpaces(scenarioData.getJSONArray("spaces"), "injector-dataset");
        injectorMonitor.stop();
      }

      if (scenarioData.has("activities")) {

        LOG.info("Create " + scenarioData.getJSONArray("activities").length() + " activities.");
        injectorMonitor.start("Processing users data");
        activityModule.pushActivities(scenarioData.getJSONArray("activities"));
        injectorMonitor.stop();
      }

      LOG.info("Data Injection has been done successfully.............");
      LOG.info(injectorMonitor.prettyPrint());

    } catch (JSONException e) {
      LOG.error("Syntax error when reading scenario " + scenarioName, e);
    }

  }

  /**
   * Gets the data.
   *
   * @param inputStream the input stream
   * @return the data
   */
  public String getData(InputStream inputStream) {
    String out = "";
    StringWriter writer = new StringWriter();
    try {
      IOUtils.copy(inputStream, writer);
      out = writer.toString();

    } catch (IOException e) {
      e.printStackTrace(); // To change body of catch statement use File |
                           // Settings | File Templates.
    }

    return out;
  }

}
