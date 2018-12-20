package org.exoplatform.datacollector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.Test;

import com.google.common.primitives.Doubles;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class SocialDataCollectorServiceTest extends BaseActivityTestCase {

  /** Logger */
  private static final Log           LOG          = ExoLogger.getExoLogger(SocialDataCollectorServiceTest.class);

  private static final String        TEXT_PATTERN = ".*[a-zA-Z]+.*";

  private static AtomicReference<String>                activitiesFile = new AtomicReference<>();

  private SocialDataCollectorService dataCollector;

  private BufferedReader             activitiesReader;

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
        dataCollector.collectUserActivities(writer);
        writer.close();
        activitiesFile.set(file.getAbsolutePath());
      } catch (Exception e) {
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
    String validHeader = "id,title,type_content,type_social,type_calendar,type_forum,type_wiki,type_poll,type_other,"
        + "owner_id,owner_title,owner_type_organization,owner_type_space,owner_influence,number_of_likes,number_of_comments,"
        + "reactivity,is_mentions_me,is_mentions_connections,is_commented_by_me,is_commented_by_connetions,is_liked_by_me,is_liked_by_connections,"
        + "poster_id,poster_gender_male,poster_gender_female,poster_is_employee,poster_is_lead,"
        + "poster_is_in_connections,poster_focus_engineering,poster_focus_sales,poster_focus_marketing,poster_focus_management,poster_focus_financial,poster_focus_other,poster_influence,"
        + "participant1_id,participant1_conversed,participant1_favored,participant1_gender_male,participant1_gender_female,participant1_is_employee,participant1_is_lead,participant1_is_in_connections,participant1_focus_engineering,participant1_focus_sales,participant1_focus_marketing,participant1_focus_management,participant1_focus_financial,participant1_focus_other,participant1_influence,"
        + "participant2_id,participant2_conversed,participant2_favored,participant2_gender_male,participant2_gender_female,participant2_is_employee,participant2_is_lead,participant2_is_in_connections,participant2_focus_engineering,participant2_focus_sales,participant2_focus_marketing,participant2_focus_management,participant2_focus_financial,participant2_focus_other,participant2_influence,"
        + "participant3_id,participant3_conversed,participant3_favored,participant3_gender_male,participant3_gender_female,participant3_is_employee,participant3_is_lead,participant3_is_in_connections,participant3_focus_engineering,participant3_focus_sales,participant3_focus_marketing,participant3_focus_management,participant3_focus_financial,participant3_focus_other,participant3_influence,"
        + "participant4_id,participant4_conversed,participant4_favored,participant4_gender_male,participant4_gender_female,participant4_is_employee,participant4_is_lead,participant4_is_in_connections,participant4_focus_engineering,participant4_focus_sales,participant4_focus_marketing,participant4_focus_management,participant4_focus_financial,participant4_focus_other,participant4_influence,"
        + "participant5_id,participant5_conversed,participant5_favored,participant5_gender_male,participant5_gender_female,participant5_is_employee,participant5_is_lead,participant5_is_in_connections,participant5_focus_engineering,participant5_focus_sales,participant5_focus_marketing,participant5_focus_management,participant5_focus_financial,participant5_focus_other,participant5_influence";
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

  // @Test
  public void _testDataFormat() {
    // TODO in this test use a table: Map of Lists, a key it's Column title, the
    // column value it's a List with related values
    // Or Map of indexes: a key it's Column title, a value is an index of the
    // column value in array of data in each read line split by comma.
    // 1) Read all the dataset in the table
    // 2) Get column by a name and check the values type

    activitiesReader.lines().skip(1).forEach(line -> {
      String[] columns = line.split(",");
      if (columns.length >= 111) {

        // If you've added or removed any column, make sure the ranges and data
        // types are correct.

        // Ranges of bytes
        Stream<String> bitRange1 = Arrays.stream(Arrays.copyOfRange(columns, 2, 9));
        Stream<String> bitRange2 = Arrays.stream(Arrays.copyOfRange(columns, 11, 13));
        Stream<String> bitRange3 = Arrays.stream(Arrays.copyOfRange(columns, 17, 23));
        Stream<String> bitRange4 = Arrays.stream(Arrays.copyOfRange(columns, 24, 34));
        Stream<String> bitRange5 = Arrays.stream(Arrays.copyOfRange(columns, 36, 48));
        Stream<String> bitRange6 = Arrays.stream(Arrays.copyOfRange(columns, 50, 62));
        Stream<String> bitRange7 = Arrays.stream(Arrays.copyOfRange(columns, 64, 76));
        Stream<String> bitRange8 = Arrays.stream(Arrays.copyOfRange(columns, 78, 90));
        Stream<String> bitRange9 = Arrays.stream(Arrays.copyOfRange(columns, 92, 104));

        // Range of strings. Column numbers: 0, 1, 9, 10, 23, 35, 49, 63, 77, 91
        Stream<String> stringRange = Stream.of(columns[0],
                                               columns[1],
                                               columns[9],
                                               columns[10],
                                               columns[23],
                                               columns[35],
                                               columns[49],
                                               columns[63],
                                               columns[77],
                                               columns[91]);

        // Range of floats 13, 14, 15, 16, 34, 48, 62, 76, 90, 104
        Stream<String> floatRange = Stream.of(columns[13],
                                              columns[14],
                                              columns[15],
                                              columns[16],
                                              columns[34],
                                              columns[48],
                                              columns[62],
                                              columns[76],
                                              columns[90],
                                              columns[104]);

        assertTrue(bitRange1.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitRange2.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitRange3.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitRange4.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitRange5.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitRange6.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitRange7.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitRange8.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitRange9.allMatch(elem -> elem.equals("1") || elem.equals("0")));

        assertTrue(stringRange.allMatch(elem -> elem.matches(TEXT_PATTERN)));

        assertTrue(floatRange.allMatch(elem -> Doubles.tryParse(elem) != null));
        assertTrue(floatRange.allMatch(elem -> {
          Double value = Double.parseDouble(elem);
          return value >= 0 && value <= 1;
        }));

      }
    });
  }

}
