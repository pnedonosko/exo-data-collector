package org.exoplatform.datacollector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.Test;

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
  private static final Log           LOG = ExoLogger.getExoLogger(SocialDataCollectorServiceTest.class);

  private SocialDataCollectorService dataCollector;

  private BufferedReader             activitiesReader;

  @Override
  protected void beforeClass() {
    super.beforeClass();

    //
    dataCollector = container.getComponentInstanceOfType(SocialDataCollectorService.class);

    //
    try {
      File activitiesFile = File.createTempFile("data_collector", ".csv");
      PrintWriter writer = new PrintWriter(activitiesFile);

      dataCollector.collectUserActivities(writer);
      writer.close();

      activitiesReader = new BufferedReader(new FileReader(activitiesFile));
    } catch (Exception e) {
      fail("Error collecting activities", e);
    }
  }

  @Test
  public void testHeader() throws IOException {
    String validHeader = "id,title,type_content,type_social,type_calendar,type_forum,type_wiki,type_poll,type_other,owner_id,owner_title,owner_type_organization,owner_type_space,owner_influence,number_of_likes,number_of_comments,reactivity,is_mentions_me,is_mentions_connections,is_commented_by_me,is_commented_by_connetions,is_liked_by_me,is_liked_by_connections,poster_id,poster_gender,poster_is_employee,poster_is_lead,poster_is_in_connections,poster_focus_engineering,poster_focus_sales,poster_focus_marketing,poster_focus_management,poster_focus_financial,poster_focus_other,poster_influence,participant1_id,participant1_conversed,participant1_favored,participant1_gender,participant1_is_employee,participant1_is_lead,participant1_is_in_connections,participant1_focus_engineering,participant1_focus_sales,participant1_focus_marketing,participant1_focus_management,participant1_focus_financial,participant1_focus_other,participant1_influence,participant2_id,participant2_conversed,participant2_favored,participant2_gender,participant2_is_employee,participant2_is_lead,participant2_is_in_connections,participant2_focus_engineering,participant2_focus_sales,participant2_focus_marketing,participant2_focus_management,participant2_focus_financial,participant2_focus_other,participant2_influence,participant3_id,participant3_conversed,participant3_favored,participant3_gender,participant3_is_employee,participant3_is_lead,participant3_is_in_connections,participant3_focus_engineering,participant3_focus_sales,participant3_focus_marketing,participant3_focus_management,participant3_focus_financial,participant3_focus_other,participant3_influence,participant4_id,participant4_conversed,participant4_favored,participant4_gender,participant4_is_employee,participant4_is_lead,participant4_is_in_connections,participant4_focus_engineering,participant4_focus_sales,participant4_focus_marketing,participant4_focus_management,participant4_focus_financial,participant4_focus_other,participant4_influence,participant5_id,participant5_conversed,participant5_favored,participant5_gender,participant5_is_employee,participant5_is_lead,participant5_is_in_connections,participant5_focus_engineering,participant5_focus_sales,participant5_focus_marketing,participant5_focus_management,participant5_focus_financial,participant5_focus_other,participant5_influence";
    String header = activitiesReader.readLine();
    assertEquals(validHeader, header);
  }
  
  @Test
  public void testColumnsNumber() throws IOException {
    activitiesReader.lines().forEach(line -> {
      String[] array = line.split(","); 
      assertEquals(105, array.length);
    });
  }

  @Test
  public void testDataFormat() {
    activitiesReader.lines().skip(1).forEach(line -> {
      String[] array = line.split(","); 
      if(array.length > 104) {
        
        // Ranges of bytes
        
        Stream<String> bitPart1 = Arrays.stream(Arrays.copyOfRange(array, 2, 9));  
        Stream<String> bitPart2 = Arrays.stream(Arrays.copyOfRange(array, 11, 13)); 
        Stream<String> bitPart3 = Arrays.stream(Arrays.copyOfRange(array, 17, 23));
        Stream<String> bitPart4 = Arrays.stream(Arrays.copyOfRange(array, 24, 34));
        Stream<String> bitPart5 = Arrays.stream(Arrays.copyOfRange(array, 36, 48));
        Stream<String> bitPart6 = Arrays.stream(Arrays.copyOfRange(array, 50, 62));
        Stream<String> bitPart7 = Arrays.stream(Arrays.copyOfRange(array, 64, 76));
        Stream<String> bitPart8 = Arrays.stream(Arrays.copyOfRange(array, 78, 90));
        Stream<String> bitPart9 = Arrays.stream(Arrays.copyOfRange(array, 92, 104));
        
        assertTrue(bitPart1.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitPart2.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitPart3.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitPart4.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitPart5.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitPart6.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitPart7.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitPart8.allMatch(elem -> elem.equals("1") || elem.equals("0")));
        assertTrue(bitPart9.allMatch(elem -> elem.equals("1") || elem.equals("0")));
       
      }
    });
  } 

}
