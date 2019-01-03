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
package org.exoplatform.prediction;

import java.io.File;
import java.util.Date;

import org.picocontainer.Startable;

import org.exoplatform.portal.pom.data.ModelData;
import org.exoplatform.prediction.user.dao.ModelEntityDAO;
import org.exoplatform.prediction.user.domain.ModelEntity;
import org.exoplatform.prediction.user.domain.ModelEntity.Status;
import org.exoplatform.prediction.user.domain.ModelId;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Train ML models using users data and save them in the storage. Created by The
 * eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: TrainingService.java 00000 Dec 14, 2018 pnedonosko $
 */
public class TrainingService implements Startable {

  /** Logger */
  private static final Log       LOG = ExoLogger.getExoLogger(TrainingService.class);

  /** ModelEntityDAO */
  protected final ModelEntityDAO modelEntityDAO;

  /**
   * Instantiates a new training service.
   */
  public TrainingService(ModelEntityDAO modelEntityDAO) {
    this.modelEntityDAO = modelEntityDAO;

  }

  /**
   * Submit a new model to train in the training service.
   *
   * @param userName the user name
   * @param datasetFile the dataset file
   */
  public void addModel(String userName, String datasetFile) {

    // TODO
    // 1) Submit a training task to a queue (DB: ModelEntity), using user name
    // to build a model name. The task is asynchronous.
    // 2) Later, it should be possible to find a status of the model
    // 3) Respect current model status: if it's NEW - need cleanup it, if it's
    // PROCESSING - need cancel it, if it's READY - create a NEW version and
    // when will be processed mark as READY also.

    ModelEntity modelEntity = new ModelEntity(userName, datasetFile);
    Long lastVersion = modelEntityDAO.findLastModelVersion(userName);

    if (lastVersion != null) {
      ModelEntity currentModel = modelEntityDAO.find(new ModelId(userName, lastVersion));

      if (currentModel.getStatus() == Status.NEW || currentModel.getStatus() == Status.PROCESSING) {
        if (currentModel.getDatasetFile() != null) {
          new File(currentModel.getDatasetFile()).delete();
        }
        if (currentModel.getModelFile() != null) {
          new File(currentModel.getModelFile()).delete();
        }

        modelEntityDAO.delete(currentModel);
      }
    }

    modelEntityDAO.create(modelEntity);
  }

  /**
   * Gets the model's last actual status.
   *
   * @return the model status
   */
  public Status getModelStatus(String userName, Long version) {
    return modelEntityDAO.findStatusByNameAndVersion(userName, version);
  }

  public void activateModel(String userName, Long version, String modelFile) {
    // TODO: archive old model
    ModelEntity modelEntity = modelEntityDAO.find(new ModelId(userName, version));
    if (modelEntity != null) {
      modelEntity.setActivated(new Date());
      modelEntity.setModelFile(modelFile);
      modelEntityDAO.update(modelEntity);
    } else {
      LOG.warn("Cannot activate the model (name: " + userName + ", version: " + version + ") - the model not found");
    }

  }
  
  public ModelEntity getLastModel(String userName) {
    Long lastVersion = modelEntityDAO.findLastModelVersion(userName);
    if(lastVersion == null) {
      return null;
    }
    
    return modelEntityDAO.find(new ModelId(userName, lastVersion));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {

  }

}
