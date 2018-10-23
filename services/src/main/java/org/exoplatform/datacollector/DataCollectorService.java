/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.datacollector;

import java.util.Date;

import org.exoplatform.datacollector.dao.RelevanceDAO;
import org.exoplatform.datacollector.domain.RelevanceEntity;
import org.exoplatform.datacollector.domain.RelevanceId;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.picocontainer.Startable;

/**
 * The Class DataCollectorService.
 */
public class DataCollectorService implements Startable {

	/** The DAO for RelevanceEntity */
	protected final RelevanceDAO relevanceStorage;

	/** The Constant LOG. */
	protected static final Log LOG = ExoLogger.getLogger(DataCollectorService.class);

	/**
	 * Instantiates a Data collector Service
	 * 
	 * @param relevanceStorage is the DAO for RelevanceEntity
	 */
	public DataCollectorService(RelevanceDAO relevanceStorage) {
		this.relevanceStorage = relevanceStorage;
	}

	/**
	 * Instantiates a Data collector Service
	 * 
	 * @param jcrService
	 * @param sessionProviders
	 * @param hierarchyCreator
	 * @param organization
	 * @param identityManager
	 * @param identityStorage
	 * @param activityManager
	 * @param relevanceStorage
	 */
	public DataCollectorService(RepositoryService jcrService,
            					SessionProviderService sessionProviders,
            					NodeHierarchyCreator hierarchyCreator,
            					OrganizationService organization,
            					IdentityManager identityManager,
            					IdentityStorage identityStorage,
            					ActivityManager activityManager,
            					RelevanceDAO relevanceStorage) {
		
		this.relevanceStorage = relevanceStorage;
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() {
		// Nothing

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
		// Nothing

	}

	/**
	 * Saves a relevance to the storage. 
	 * Updates the relevance if it already exists.
	 * 
	 * @param relevance to be saved/updated
	 */
	public void saveRelevance(RelevanceEntity relevance) {
		RelevanceEntity existingRelevance = relevanceStorage
				.find(new RelevanceId(relevance.getUserId(), relevance.getActivityId()));
		
		relevance.setUpdateDate(new Date());
		
		if (existingRelevance == null) {
			relevanceStorage.create(relevance);
			LOG.info("Relevance created: " + relevance);
		} else {
			relevanceStorage.update(relevance);
			LOG.info("Relevance updated: " + relevance);
		}
	}

	/**
	 * Gets RelevanceEntity by given RelevanceId which contains the user id and
	 * activity id.
	 *
	 * @param relevanceId is the searching parameter for retrieving RelevanceEntity
	 * @return found RelevanceEntity or null if there is no such RelevanceEntity.
	 */
	public RelevanceEntity findById(RelevanceId relevanceId) {
		return relevanceStorage.find(relevanceId);
	}

}
