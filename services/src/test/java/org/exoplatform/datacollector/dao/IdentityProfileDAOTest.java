package org.exoplatform.datacollector.dao;

import org.junit.Test;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.datacollector.BaseActivityTestCase;
import org.exoplatform.datacollector.domain.IdentityProfileEntity;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class IdentityProfileDAOTest extends BaseActivityTestCase {

  private IdentityProfileDAO identityProfileDAO;

  @Override
  protected void beforeClass() {
    super.beforeClass();
    identityProfileDAO = getService(IdentityProfileDAO.class);
    
    identityProfileDAO.deleteAll();
    
    IdentityProfileEntity profileJohn = new IdentityProfileEntity("1", "john", "space_employees", "marketing", "gender:male");
    IdentityProfileEntity profileMary = new IdentityProfileEntity("2", "mary", "space_marketing", "sales", "gender:female");
    IdentityProfileEntity profileJack = new IdentityProfileEntity("3", "jack", "space_product", "product", "gender:male");
    identityProfileDAO.create(profileJohn);
    identityProfileDAO.create(profileMary);
    identityProfileDAO.create(profileJack);
  }

  @Test
  public void testFindById() {
    IdentityProfileEntity john = identityProfileDAO.findById("1");
    assertEquals("1", john.getId());
    assertEquals("john", john.getName());
    assertEquals("space_employees",john.getProviderId());
    assertEquals("marketing", john.getFocus());
    assertEquals("gender:male", john.getContext());

  }

  @Test
  public void testFindByName() {
    IdentityProfileEntity mary = identityProfileDAO.findByName("mary");
    assertEquals("2", mary.getId());
    assertEquals("mary", mary.getName());
    assertEquals("space_marketing",mary.getProviderId());
    assertEquals("sales", mary.getFocus());
    assertEquals("gender:female", mary.getContext());

  }
}
