package org.exoplatform.datacollector.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.datacollector.AbstractTest;
import org.exoplatform.datacollector.DataCollectorService;
import org.exoplatform.datacollector.TestUtils;
import org.exoplatform.datacollector.dao.RelevanceDAO;
import org.exoplatform.datacollector.domain.RelevanceEntity;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class DataCollectorServiceTest extends AbstractTest {
	PortalContainer container = null;
	
	DataCollectorService dataCollectorService;

	RelevanceDAO relevanceStorage;
	

	@Before
	public void setUp() {
		container = PortalContainer.getInstance();
		RepositoryService jcrService = container.getComponentInstanceOfType(RepositoryService.class);
		SessionProviderService sessionProviders = container.getComponentInstanceOfType(SessionProviderService.class);
		NodeHierarchyCreator hierarchyCreator = container.getComponentInstanceOfType(NodeHierarchyCreator.class);
		OrganizationService organization = container.getComponentInstanceOfType(OrganizationService.class);
		IdentityManager identityManager = container.getComponentInstanceOfType(IdentityManager.class);
		IdentityStorage identityStorage = container.getComponentInstanceOfType(IdentityStorage.class);
		ActivityManager activityManager = container.getComponentInstanceOfType(ActivityManager.class);
		relevanceStorage = mock(RelevanceDAO.class);
		
		/*
		System.out.println("activityManager: " + activityManager);
		System.out.println("identityStorage: " + identityStorage);
		System.out.println("sessionProviders: " + sessionProviders);
		System.out.println("jcrService: " + jcrService);
		System.out.println("hierarchyCreator: " + hierarchyCreator);
		System.out.println("organization: " + organization);
		System.out.println("identityManager: " + identityManager);
		System.out.println("relevanceStorage: " + relevanceStorage);
		*/
		
		dataCollectorService = new DataCollectorService(jcrService, sessionProviders, hierarchyCreator, organization, identityManager, identityStorage, activityManager, relevanceStorage);
		
		when(relevanceStorage.find(TestUtils.EXISTING_RELEVANCE_ID)).thenReturn(TestUtils.getExistingRelevance());
		when(relevanceStorage.find(TestUtils.UNEXISTING_RELEVANCE_ID)).thenReturn(null);
	}

	@Test
	public void testSaveRelevanceUpdate() {
		RelevanceEntity relevance = TestUtils.getExistingRelevance();
		dataCollectorService.saveRelevance(relevance);
		
		verify(relevanceStorage, times(1)).update(relevance);
	}

	@Test
	public void testSaveRelevance() {
		RelevanceEntity relevance = TestUtils.getNewRelevance();
		dataCollectorService.saveRelevance(relevance);
		
		verify(relevanceStorage, times(1)).create(relevance);
	}
	
	@Test
	public void testFindById() {
		dataCollectorService.findById(TestUtils.EXISTING_RELEVANCE_ID);
		
		verify(relevanceStorage, times(1)).find(TestUtils.EXISTING_RELEVANCE_ID);
	}
	
	@After
	public void tearDown() {
		dataCollectorService = null;
	}
}
