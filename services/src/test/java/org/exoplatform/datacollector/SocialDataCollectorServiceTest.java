package org.exoplatform.datacollector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

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

  private BufferedReader activitiesReader;
  
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
    String header = activitiesReader.readLine();
    
    
  }

}
