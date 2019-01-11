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
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;

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
  private static final Log       LOG                  = ExoLogger.getExoLogger(TrainingService.class);

  /** Max count of archived models in DB  */
  private static final Integer   MAX_STORED_MODELS    = 20;

  private static final String    TRAINING_SCRIPT_PATH = "/tmp/train.sh";

  /** ModelEntityDAO */
  protected final ModelEntityDAO modelEntityDAO;

  /**
   * Instantiates a new training service.
   */
  public TrainingService(ModelEntityDAO modelEntityDAO) {
    this.modelEntityDAO = modelEntityDAO;

  }

  /**
   * Submits a new model to train in the training service.
   * Respects current model status:
   * If NEW or PROCESSING - cleans up it
   * If READY - creates a NEW version
   * @param userName the user name
   * @param datasetFile the dataset file
   */
  public void addModel(String userName, String datasetFile) {
    ModelEntity currentModel = getLastModel(userName);
    if (currentModel != null) {
      if (currentModel.getStatus() == Status.NEW || currentModel.getStatus() == Status.PROCESSING) {
        deleteModel(currentModel);
      }
    }

    ModelEntity newModel = new ModelEntity(userName, datasetFile);
    modelEntityDAO.create(newModel);
  }

  /**
   * Gets the model's actual status.
   *
   * @return the model status
   */
  public Status getModelStatus(String userName, Long version) {
    return modelEntityDAO.findStatusByNameAndVersion(userName, version);
  }

  /**
   * Activates model by setting the modelFile, activatedDate, READY status
   * Archives the old version of model, if exists.
   * Deletes the dataset file.
   * @param userName of the model
   * @param version of the model
   * @param modelFile to be set up
   */
  public void activateModel(String userName, Long version, String modelFile) {
    ModelEntity modelEntity = modelEntityDAO.find(new ModelId(userName, version));
    if (modelEntity != null) {
      String dataset = modelEntity.getDatasetFile();
      modelEntity.setActivated(new Date());
      modelEntity.setModelFile(modelFile);
      modelEntity.setDatasetFile(null);
      modelEntity.setStatus(Status.READY);
      modelEntityDAO.update(modelEntity);
      // Archive old version
      if (version != 1L) {
        archiveModel(userName, version - 1L);
      }
      // Delete the dataset file
      if (dataset != null) {
        new File(dataset).delete();
      }
    } else {
      LOG.warn("Cannot activate model (name: {}, version: {}) - the model not found", userName, version);
    }

  }

  /**
   * Archives a model by setting archived date.
   * Deletes old models
   * @param userName of the model
   * @param version of the model
   */
  public void archiveModel(String userName, Long version) {
    ModelEntity model = modelEntityDAO.find(new ModelId(userName, version));
    if (model != null) {
      model.setArchived(new Date());
      modelEntityDAO.update(model);
      LOG.info("Model (name: " + userName + ", version: " + version + ") archived");

      // Delete old models (version = current version - MAX_STORED_MODELS to be
      // deleted)
      ModelEntity oldModel = modelEntityDAO.find(new ModelId(userName, version - MAX_STORED_MODELS));
      deleteModel(oldModel);
    } else {
      LOG.warn("Cannot archive model (name: {}, version: {}) - the model not found", userName, version);
    }
  }

  /**
   * Gets the model with latest version
   * @param userName
   * @return modelEntity or null
   */
  public ModelEntity getLastModel(String userName) {
    Long lastVersion = modelEntityDAO.findLastModelVersion(userName);
    if (lastVersion == null) {
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

  /**
   * Deletes a model from DB and clears its files
   * @param model
   */
  private void deleteModel(ModelEntity model) {
    if (model != null) {
      if (model.getDatasetFile() != null) {
        new File(model.getDatasetFile()).delete();
      }
      if (model.getModelFile() != null) {
        try {
          FileUtils.deleteDirectory(new File(model.getModelFile()));
        } catch (IOException e) {
          LOG.warn("Cannot delete model folder: " + model.getModelFile());
        }
      }

      modelEntityDAO.delete(model);
    }
  }

  public void trainModel(File dataset, String userName) {
    File modelFolder = new File(dataset.getParentFile().getAbsolutePath() + "/model");
    modelFolder.mkdirs();

    String[] cmd = { "python", TRAINING_SCRIPT_PATH, dataset.getAbsolutePath(), modelFolder.getAbsolutePath() };
    try {
      LOG.info("Running Python script....");
      Process trainingProcess = Runtime.getRuntime().exec(cmd);
      trainingProcess.waitFor();
      LOG.info("Model successfuly trained");

      // TODO check if model trained without errors
      ModelEntity model = getLastModel(userName);
      if (model != null) {
        activateModel(userName, model.getVersion(), modelFolder.getAbsolutePath());
      }

    } catch (IOException e) {
      LOG.error("Cannot execure external training script: ", e.getMessage());
    } catch (InterruptedException e) {
      LOG.warn("Training process has been interrupted");
    }
  }
}
