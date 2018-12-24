package org.exoplatform.prediction.user.dao;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.prediction.user.domain.ModelEntity;
import org.exoplatform.prediction.user.domain.ModelEntity.Status;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class ModelEntityDAOTest extends BaseCommonsTestCase {

  private ModelEntityDAO modelEntityDAO;

  private ModelEntity    entityJohnV1, entityJohnV2, entityJohnV3, entityMaryV1, entityMaryV2;

  @Override
  protected void beforeClass() {
    super.beforeClass();
    this.container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);

    modelEntityDAO = getService(ModelEntityDAO.class);

    modelEntityDAO.deleteAll();
    initEntities();

  }

  @Test
  public void testCreate() {

    modelEntityDAO.create(entityJohnV1);
    modelEntityDAO.create(entityJohnV2);
    modelEntityDAO.create(entityJohnV3);

    modelEntityDAO.create(entityMaryV1);
    modelEntityDAO.create(entityMaryV2);

    List<ModelEntity> result = modelEntityDAO.findAll();

    assertEquals(1, result.stream().filter(model -> model.getName().equals("john") && model.getVersion() == 1).count());
    assertEquals(1, result.stream().filter(model -> model.getName().equals("john") && model.getVersion() == 2).count());
    assertEquals(1, result.stream().filter(model -> model.getName().equals("john") && model.getVersion() == 3).count());

    assertEquals(1, result.stream().filter(model -> model.getName().equals("mary") && model.getVersion() == 1).count());
    assertEquals(1, result.stream().filter(model -> model.getName().equals("mary") && model.getVersion() == 2).count());
  }

  private void initEntities() {

    entityJohnV1 = new ModelEntity();
    entityJohnV2 = new ModelEntity();
    entityJohnV3 = new ModelEntity();
    for (ModelEntity entityJohn : Arrays.asList(entityJohnV1, entityJohnV2, entityJohnV3)) {
      entityJohn.setActivated(new Date());
      entityJohn.setArchived(new Date());
      entityJohn.setCreated(new Date());
      entityJohn.setDatasetFile("datasetfile");
      entityJohn.setModelFile("modelfile");
      entityJohn.setName("john");
      entityJohn.setStatus(Status.NEW);
    }

    entityMaryV1 = new ModelEntity();
    entityMaryV2 = new ModelEntity();

    for (ModelEntity entityMary : Arrays.asList(entityMaryV1, entityMaryV2)) {
      entityMary.setActivated(new Date());
      entityMary.setArchived(new Date());
      entityMary.setCreated(new Date());
      entityMary.setDatasetFile("anotherDatasetFile");
      entityMary.setModelFile("anotherModelFile");
      entityMary.setName("mary");
      entityMary.setStatus(Status.PROCESSING);
    }
  }

}
