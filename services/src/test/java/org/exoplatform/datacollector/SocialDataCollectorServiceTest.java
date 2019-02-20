package org.exoplatform.datacollector;

import static org.exoplatform.datacollector.ListAccessUtil.loadActivitiesListIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.caja.util.Sets;
import com.google.common.primitives.Doubles;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class SocialDataCollectorServiceTest extends BaseActivityTestCase {

  /** Logger */
  private static final Log               LOG             = ExoLogger.getExoLogger(SocialDataCollectorServiceTest.class);

  private static final String            TEXT_PATTERN    = ".*[a-zA-Z]+.*";

  public static final String             DELIMITER       = ",";

  // Some of them are String in eXo Platform
  public static final String[]           INTEGER_COLUMNS = { "id", "owner_id", "poster_id", "participant1_id", "participant2_id",
      "participant3_id", "participant4_id", "participant5_id" };

  private static final String[]          STRING_COLUMNS  = { "owner_title" };

  private static final String[]          FLOAT_COLUMNS   = { "owner_influence", "number_of_likes", "number_of_comments",
      "reactivity", "poster_influence", "participant1_influence", "participant2_influence", "participant3_influence",
      "participant4_influence", "participant5_influence, rank" };

  private static AtomicReference<String> activitiesFile  = new AtomicReference<>();

  private SocialDataCollectorService     dataCollector;

  private BufferedReader                 activitiesReader;

  @Override
  protected void beforeClass() {
    super.beforeClass();

    //
    dataCollector = getService(SocialDataCollectorService.class);

    //
    if (activitiesFile.get() == null) {
      try {
        File file;
        String dataDirPath = System.getProperty("gatein.data.dir");
        if (dataDirPath != null && dataDirPath.length() > 0) {
          file = File.createTempFile("data_collector", ".csv", new File(dataDirPath));
        } else {
          file = File.createTempFile("data_collector", ".csv");
        }
        PrintWriter writer = new PrintWriter(file);

        begin();
        long sinceTime = System.currentTimeMillis() - UserInfluencers.FEED_MILLIS_RANGE;
        Identity id = dataCollector.getUserIdentityById(jasonId);
        UserSnapshot user = dataCollector.createUserSnapshot(id);
        user.initInfluencers();
        dataCollector.initializeUserSnapshot(user, sinceTime);
        Iterator<ExoSocialActivity> activities = loadActivitiesListIterator(activityManager.getActivityFeedWithListAccess(id),
                                                                            sinceTime);
        dataCollector.writeUserActivities(user, activities, writer, true);
        writer.close();
        activitiesFile.set(file.getAbsolutePath());
        LOG.info("PATH: " + file.getAbsolutePath());
      } catch (Exception e) {
        LOG.error("Error during collecting user activities", e);
        fail("Error collecting activities", e);
      } finally {
        end();
      }
    }

    try {
      activitiesReader = new BufferedReader(new FileReader(activitiesFile.get()));
    } catch (Exception e) {
      fail("Error reading collected activities", e);
    }
  }

  @Test
  public void testHeader() throws IOException {
    String validHeader = "id,type_content,type_social,type_calendar,type_forum,type_wiki,type_poll,type_other,"
        + "owner_id,owner_title,owner_type_organization,owner_type_space,owner_influence,number_of_likes,number_of_comments,"
        + "reactivity,is_mentions_me,is_mentions_connections,is_commented_by_me,is_commented_by_connetions,is_liked_by_me,is_liked_by_connections,"
        + "poster_id,poster_gender_male,poster_gender_female,poster_is_employee,poster_is_lead,"
        + "poster_is_in_connections,poster_focus_engineering,poster_focus_sales,poster_focus_marketing,poster_focus_management,poster_focus_financial,poster_focus_other,poster_influence,"
        + "participant1_id,participant1_conversed,participant1_favored,participant1_gender_male,participant1_gender_female,participant1_is_employee,participant1_is_lead,participant1_is_in_connections,participant1_focus_engineering,participant1_focus_sales,participant1_focus_marketing,participant1_focus_management,participant1_focus_financial,participant1_focus_other,participant1_influence,"
        + "participant2_id,participant2_conversed,participant2_favored,participant2_gender_male,participant2_gender_female,participant2_is_employee,participant2_is_lead,participant2_is_in_connections,participant2_focus_engineering,participant2_focus_sales,participant2_focus_marketing,participant2_focus_management,participant2_focus_financial,participant2_focus_other,participant2_influence,"
        + "participant3_id,participant3_conversed,participant3_favored,participant3_gender_male,participant3_gender_female,participant3_is_employee,participant3_is_lead,participant3_is_in_connections,participant3_focus_engineering,participant3_focus_sales,participant3_focus_marketing,participant3_focus_management,participant3_focus_financial,participant3_focus_other,participant3_influence,"
        + "participant4_id,participant4_conversed,participant4_favored,participant4_gender_male,participant4_gender_female,participant4_is_employee,participant4_is_lead,participant4_is_in_connections,participant4_focus_engineering,participant4_focus_sales,participant4_focus_marketing,participant4_focus_management,participant4_focus_financial,participant4_focus_other,participant4_influence,"
        + "participant5_id,participant5_conversed,participant5_favored,participant5_gender_male,participant5_gender_female,participant5_is_employee,participant5_is_lead,participant5_is_in_connections,participant5_focus_engineering,participant5_focus_sales,participant5_focus_marketing,participant5_focus_management,participant5_focus_financial,participant5_focus_other,participant5_influence,rank";
    String header = activitiesReader.readLine();
    assertEquals(validHeader, header);
  }

  @Test
  public void testColumnsNumber() throws IOException {
    activitiesReader.lines().forEach(line -> {
      String[] array = line.split(",");
      assertEquals(111, array.length);
    });
  }

  @Test
  public void testDataFormat() {

    List<String> lines = activitiesReader.lines().collect(Collectors.toList());
    Map<String, List<String>> dataset = new HashMap<String, List<String>>();
    String[] columnTitles = lines.get(0).split(DELIMITER);

    for (String title : columnTitles) {
      dataset.put(title, new ArrayList<>());
    }

    // Init dataset
    lines.stream().skip(1).forEach(line -> {
      String[] columns = line.split(DELIMITER);
      for (int i = 0; i < columns.length; i++) {
        dataset.get(columnTitles[i]).add(columns[i]);
      }
    });

    // INTEGERS checks
    for (String title : INTEGER_COLUMNS) {
      assertTrue(dataset.get(title).stream().allMatch(value -> {
        try {
          Integer.parseInt(value);
        } catch (Exception e) {
          return false;
        }
        return true;
      }));
    }

    // STRINGS check
    for (String title : STRING_COLUMNS) {
      assertTrue(dataset.get(title).stream().allMatch(value -> {
        return value.matches(TEXT_PATTERN);
      }));
    }

    // FLOATS check
    for (String title : FLOAT_COLUMNS) {
      assertTrue(dataset.get(title).stream().allMatch(value -> {
        return Doubles.tryParse(value) != null;
      }));
    }

    // There are a lot of bit columns. Consider all columns that are not STRING,
    // INTEGER or FLOAT columns as BIT columns.
    Set<String> bitColumns = Sets.newHashSet(columnTitles);
    bitColumns.removeAll(Arrays.asList(INTEGER_COLUMNS));
    bitColumns.removeAll(Arrays.asList(FLOAT_COLUMNS));
    bitColumns.removeAll(Arrays.asList(STRING_COLUMNS));

    // BITS check
    bitColumns.forEach(title -> {
      assertTrue(dataset.get(title).stream().allMatch(value -> value.equals("1") || value.equals("0")));
    });

  }

}
