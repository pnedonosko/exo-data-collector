package org.exoplatform.prediction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.prediction.user.dao.ModelEntityDAO;
import org.exoplatform.prediction.user.domain.ModelEntity;
import org.exoplatform.prediction.user.domain.ModelEntity.Status;
import org.exoplatform.prediction.user.domain.ModelId;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class TrainingServiceTest extends BaseCommonsTestCase {

  /** Logger */
  private static final Log  LOG = ExoLogger.getExoLogger(TrainingServiceTest.class);

  protected TrainingService trainingService;

  protected ModelEntityDAO  modelEntityDAO;

  @Override
  protected void beforeClass() {
    super.beforeClass();
    this.container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);

    trainingService = getService(TrainingService.class);
    modelEntityDAO = getService(ModelEntityDAO.class);

    modelEntityDAO.deleteAll();
    initModels();
  }

  @Test
  public void testAddModel() {
    ModelEntity johnModel = modelEntityDAO.find(new ModelId("john", 1L));
    ModelEntity maryModel = modelEntityDAO.find(new ModelId("mary", 1L));
    ModelEntity jasonModel = modelEntityDAO.find(new ModelId("jason", 1L));
    // Get the paths to check if they exists
    Path johnDatasetFile = Paths.get(johnModel.getDatasetFile());
    Path maryDatasetFile = Paths.get(maryModel.getDatasetFile());
    Path maryModelFile = Paths.get(maryModel.getModelFile());
    Path jasonDatasetFile = Paths.get(jasonModel.getDatasetFile());
    Path jasonModelFile = Paths.get(jasonModel.getModelFile());

    assertTrue(Files.exists(johnDatasetFile));
    assertTrue(Files.exists(maryDatasetFile));
    assertTrue(Files.exists(maryModelFile));
    assertTrue(Files.exists(jasonDatasetFile));
    assertTrue(Files.exists(jasonModelFile));

    // This should delete johnDatasetFile and create a new one,
    // delete maryDatasetFile and maryModelFile, and create new maryDatasetFile
    // create new jasonDatasetFile, not to delete existing ones
    trainingService.addModel("john", createTempFile("john_dataset"));
    trainingService.addModel("mary", createTempFile("mary_dataset"));
    trainingService.addModel("jason", createTempFile("jason_dataset"));

    // Update entities link
    johnModel = modelEntityDAO.find(new ModelId("john", 1L));
    maryModel = modelEntityDAO.find(new ModelId("mary", 1L));
    jasonModel = modelEntityDAO.find(new ModelId("jason", 1L));
    ModelEntity jasonModelV2 = modelEntityDAO.find(new ModelId("jason", 2L));

    assertTrue(Files.notExists(johnDatasetFile));
    assertTrue(Files.notExists(maryDatasetFile));
    assertTrue(Files.notExists(maryModelFile));
    assertTrue(Files.exists(jasonDatasetFile));
    assertTrue(Files.exists(jasonModelFile));

    assertTrue(johnModel.getStatus() == Status.NEW && !johnModel.getDatasetFile().equals(johnDatasetFile.toString()));
    assertTrue(maryModel.getStatus() == Status.NEW && !maryModel.getDatasetFile().equals(maryDatasetFile.toString()));
    assertTrue(jasonModel.getModelFile().equals(jasonModelFile.toString()));
    assertTrue(jasonModel.getDatasetFile().equals(jasonDatasetFile.toString()));
    assertTrue(jasonModelV2.getStatus() == Status.NEW && jasonModelV2.getModelFile() == null);

  }

  protected void initModels() {
    ModelEntity johnModel = new ModelEntity("john", createTempFile("john_dataset"));

    ModelEntity maryModel = new ModelEntity("mary", createTempFile("mary_dataset"));
    maryModel.setStatus(Status.PROCESSING);
    maryModel.setModelFile(createTempFile("mary_model"));

    ModelEntity jasonModel = new ModelEntity("jason", createTempFile("jason_dataset"));
    jasonModel.setStatus(Status.READY);
    jasonModel.setModelFile(createTempFile("jason_model"));

    modelEntityDAO.create(johnModel);
    modelEntityDAO.create(maryModel);
    modelEntityDAO.create(jasonModel);

  }

  /**
   * Creates a temp file and returns it's path. 
   * @param filename
   * @return
   */
  protected String createTempFile(String filename) {
    String dataDirPath = System.getProperty("gatein.data.dir");
    File file;
    try {
      if (dataDirPath != null && dataDirPath.length() > 0) {
        file = File.createTempFile(filename, ".tmp", new File(dataDirPath));
      } else {
        file = File.createTempFile(filename, ".tmp");
      }
    } catch (IOException e) {
      LOG.error(e.getMessage());
      return null;
    }

    return file.getPath();
  }

}
