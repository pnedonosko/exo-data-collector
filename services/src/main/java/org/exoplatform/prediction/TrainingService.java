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
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.picocontainer.Startable;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.prediction.model.dao.ModelEntityDAO;
import org.exoplatform.prediction.model.domain.ModelEntity;
import org.exoplatform.prediction.model.domain.ModelEntity.Status;
import org.exoplatform.prediction.model.domain.ModelId;
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
  private static final Log       LOG               = ExoLogger.getExoLogger(TrainingService.class);

  /** Max count of archived models in DB */
  private static final Integer   MAX_STORED_MODELS = 20;

  protected final FileStorage    fileStorage;

  /** ModelEntityDAO */
  protected final ModelEntityDAO modelEntityDAO;

  protected ScriptsExecutor      scriptsExecutor;

  /**
   * Instantiates a new training service.
   *
   * @param fileStorage the file storage
   * @param modelEntityDAO the model entity DAO
   */
  public TrainingService(FileStorage fileStorage, ModelEntityDAO modelEntityDAO) {
    this.fileStorage = fileStorage;
    this.modelEntityDAO = modelEntityDAO;
  }

  /**
   * Submits a new model to train in the training service. Respects current
   * model status: If NEW or PROCESSING - cleans up it If READY - creates a NEW
   * version
   * 
   * @param userName the user name
   * @param datasetFile the dataset file
   */
  public void addModel(String userName, String dataset) {
    ModelEntity currentModel = getLastModel(userName);
    if (currentModel != null) {
      if (currentModel.getStatus() == Status.NEW || currentModel.getStatus() == Status.PROCESSING) {
        deleteModel(currentModel);
      }
    }
    ModelEntity newModel = new ModelEntity(userName, dataset);
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
   * Archives the old version of model, if exists. Deletes the dataset file.
   * 
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
   * Archives a model by setting archived date. Deletes old models
   * 
   * @param userName of the model
   * @param version of the model
   */
  public void archiveModel(String userName, Long version) {
    ModelEntity model = modelEntityDAO.find(new ModelId(userName, version));
    if (model != null) {
      model.setArchived(new Date());
      model.setStatus(Status.ARCHIEVED);
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
   * 
   * @param userName
   * @return modelEntity or null
   */
  public ModelEntity getLastModel(String userName) {
    Long lastVersion = modelEntityDAO.findLastModelVersion(userName);
    return lastVersion != null ? modelEntityDAO.find(new ModelId(userName, lastVersion)) : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    if (scriptsExecutor == null) {
      throw new RuntimeException("ScriptsExecutor is not configured");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {

  }

  /**
   * Deletes a model from DB and clears its files
   * 
   * @param model
   */
  private void deleteModel(ModelEntity model) {
    if (model != null) {
      if (model.getDatasetFile() != null) {
        new File(model.getDatasetFile()).delete();
      }
      if (model.getModelFile() != null) {
        try {
          // Delete user's folder
          FileUtils.deleteDirectory(new File(model.getModelFile()).getParentFile());
        } catch (IOException e) {
          LOG.warn("Cannot delete model folder: " + model.getModelFile());
        }
      }
      modelEntityDAO.delete(model);
    }
  }

  /**
   * Submits model training. If the training fails, trains one more time. If
   * fails again, model gets FAILED_TRAINING status
   * 
   * @param dataset
   * @param userName
   */
  public void submitTrainModel(String dataset, String userName) {
    // TODO sync manual and auto training
    setProcessing(userName);
    // If the training fails
    if (!trainModel(new File(dataset), userName)) {
      ModelEntity currentModel = getLastModel(userName);
      if (currentModel != null) {
        ModelEntity prevModel = modelEntityDAO.find(new ModelId(userName, currentModel.getVersion() - 1L));
        if (prevModel != null && Status.READY.equals(prevModel.getStatus())) {
          LOG.info("Retraining model for {}", userName);
          // Retrain model
          if (!trainModel(new File(dataset), userName)) {
            LOG.warn("Model {} failed in retraining. Set status FAILED_TRAINING", userName);
            currentModel.setStatus(Status.FAILED_TRAINING);
            modelEntityDAO.update(currentModel);
          }
        } else {
          LOG.warn("Model {} got status FAILED_TRAINING", userName);
          currentModel.setStatus(Status.FAILED_TRAINING);
          modelEntityDAO.update(currentModel);
        }
      } else {
        LOG.warn("Cannot find last model for {}", userName);
      }
    }
  }

  /**
   * Trains a model.
   * 
   * @param dataset user dataset
   * @param userName userName
   * @return true if success
   */
  protected boolean trainModel(File dataset, String userName) {
    // Train model
    String modelFolder = scriptsExecutor.train(dataset);
    // Check if model trained without errors
    File modelFile = new File(dataset.getParentFile() + "/model.json");
    if (modelFile.exists()) {
      JSONParser parser = new JSONParser();
      try {
        JSONObject resultModel = (JSONObject) parser.parse(new FileReader(modelFile));
        String result = (String) resultModel.get("status");
        if (Status.READY.name().equals(result)) {
          LOG.info("Model {} successfuly trained", userName);
          ModelEntity model = getLastModel(userName);
          activateModel(userName, model.getVersion(), modelFolder);
          return true;
        }
        LOG.warn("Model {} failed in training", userName);
      } catch (ParseException e) {
        LOG.warn("Model {} failed in training. Couldn't parse the model.json file", userName);
      } catch (IOException e) {
        LOG.warn("Model {} failed in training. Couldn't read the model.json file", userName);
      }
    }
    LOG.warn("The model.json file is not found after training for model {}", userName);
    return false;
  }

  /**
   * Adds a scriptsExecutor plugin. This method is safe in runtime: if
   * configured scriptsExecutor is not an instance of {@link ScriptsExecutor}
   * then it will log a warning and let server continue the start.
   *
   * @param plugin the plugin
   */
  public void addPlugin(ComponentPlugin plugin) {
    Class<ScriptsExecutor> pclass = ScriptsExecutor.class;
    if (pclass.isAssignableFrom(plugin.getClass())) {
      scriptsExecutor = pclass.cast(plugin);
      LOG.info("Set scripts executor instance of " + plugin.getClass().getName());
    } else {
      LOG.warn("Scripts Executor plugin is not an instance of " + pclass.getName());
    }
  }

  /**
   * Sets PROCESSING status to the last model
   * 
   * @param remoteId
   */
  public void setProcessing(String userName) {
    ModelEntity model = getLastModel(userName);
    if (model != null) {
      model.setStatus(Status.PROCESSING);
      modelEntityDAO.update(model);
      LOG.info("Model {} got status PROCESSING", userName);
    } else {
      LOG.info("Cannot set PROCESSING status to the model {} - model not found", userName);
    }
  }

  /**
   * Sets RETRY status to the last model if exists
   * 
   * @param userName the model name
   */
  public void setRetry(String userName) {
    ModelEntity model = getLastModel(userName);
    if (model != null) {
      model.setStatus(Status.RETRY);
      modelEntityDAO.update(model);
      LOG.info("Model {} got status RETRY", userName);
    } else {
      LOG.info("Cannot set RETRY status to the model {} - model not found", userName);
    }

  }

  /**
   * Updates ModelEntity
   * 
   * @param entity to be updated
   */
  public void update(ModelEntity entity) {
    modelEntityDAO.update(entity);
  }

  /**
   * Sets a dataset to the last model
   * 
   * @param userName of model
   * @param dataset to be set up
   */
  public void setDatasetToLatestModel(String userName, String dataset) {
    ModelEntity lastModel = getLastModel(userName);
    if (lastModel != null) {
      lastModel.setDatasetFile(dataset);
    }
  }

  /**
   * Gets the previous model of user
   * 
   * @param userName the name of model
   * @return previous model
   */
  public ModelEntity getPreviousModel(String userName) {
    ModelEntity lastModel = getLastModel(userName);
    return lastModel != null ? modelEntityDAO.find(new ModelId(userName, lastModel.getVersion() - 1L)) : null;
  }
}
