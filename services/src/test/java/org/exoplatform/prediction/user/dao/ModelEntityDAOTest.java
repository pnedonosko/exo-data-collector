package org.exoplatform.prediction.user.dao;

import java.util.List;

import org.junit.Test;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.prediction.model.dao.ModelEntityDAO;
import org.exoplatform.prediction.model.domain.ModelEntity;
import org.exoplatform.prediction.model.domain.ModelEntity.Status;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class ModelEntityDAOTest extends BaseCommonsTestCase {

  private ModelEntityDAO modelEntityDAO;

  private ModelEntity    entityJohnV1, entityJohnV2, entityJohnV3, entityMaryV1, entityMaryV2, entityJasonV1, entityJamesV1;

  @Override
  protected void beforeClass() {
    super.beforeClass();
    this.container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);

    modelEntityDAO = getService(ModelEntityDAO.class);

    modelEntityDAO.deleteAll();
    initEntities();

    modelEntityDAO.create(entityJohnV1);
    modelEntityDAO.create(entityJohnV2);
    modelEntityDAO.create(entityJohnV3);

    modelEntityDAO.create(entityMaryV1);
    modelEntityDAO.create(entityMaryV2);
    
    modelEntityDAO.create(entityJasonV1);
    modelEntityDAO.create(entityJamesV1);
    
  }

  @Test
  public void testCreate() {

    List<ModelEntity> result = modelEntityDAO.findAll();

    assertEquals(1, result.stream().filter(model -> model.getName().equals("john") && model.getVersion() == 1).count());
    assertEquals(1, result.stream().filter(model -> model.getName().equals("john") && model.getVersion() == 2).count());
    assertEquals(1, result.stream().filter(model -> model.getName().equals("john") && model.getVersion() == 3).count());

    assertEquals(1, result.stream().filter(model -> model.getName().equals("mary") && model.getVersion() == 1).count());
    assertEquals(1, result.stream().filter(model -> model.getName().equals("mary") && model.getVersion() == 2).count());
    
    assertEquals(1, result.stream().filter(model -> model.getName().equals("jason") && model.getVersion() == 1).count());
    assertEquals(1, result.stream().filter(model -> model.getName().equals("james") && model.getVersion() == 1).count());
  }
  
  @Test
  public void testFindStatusByNameAndVersion() {
    assertEquals(Status.NEW, modelEntityDAO.findStatusByNameAndVersion("john", 2L));
    assertEquals(Status.PROCESSING, modelEntityDAO.findStatusByNameAndVersion("jason", 1L));
    assertEquals(Status.READY, modelEntityDAO.findStatusByNameAndVersion("james", 1L));
  }

  private void initEntities() {

    entityJohnV1 = new ModelEntity("john","datasetfile");
    entityJohnV2 = new ModelEntity("john", "datasetfile");
    entityJohnV3 = new ModelEntity("john", "datasetfile");
    
    entityMaryV1 = new ModelEntity("mary", "anotherDatasetfile");
    entityMaryV2 = new ModelEntity("mary", "anotherDatasetfile");
    
    entityJasonV1 = new ModelEntity("jason", "jasonDataset");
    entityJasonV1.setStatus(Status.PROCESSING);
    
    entityJamesV1 = new ModelEntity("james", "jamesDataset");
    entityJamesV1.setStatus(Status.READY);
  }

}
