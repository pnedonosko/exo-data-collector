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
   * @param dataset the dataset
   * @return the model entity
   */
  public ModelEntity addModel(String userName, String dataset) {
    ModelEntity currentModel = getLastModel(userName);
    if (currentModel != null && (currentModel.getStatus() == Status.NEW || currentModel.getStatus() == Status.PROCESSING)) {
      // FIXME is it correct to delete here? A model added but may be still
      // collecting will be NEW, but it's correct state and we should return it
      // instead if dataset the same or raise an exception otherwise.
      deleteModel(currentModel);
      LOG.warn("Removed previously added but complete model for {}", userName);
    }
    ModelEntity newModel = new ModelEntity(userName, dataset);
    modelEntityDAO.create(newModel);
    return newModel;
  }

  /**
   * Gets the model's actual status.
   *
   * @return the model status
   */
  @Deprecated // TODO not used
  public Status getModelStatus(String userName, long version) {
    return modelEntityDAO.findStatusByNameAndVersion(userName, version);
  }

  /**
   * Activates model by setting the modelFile, activatedDate, READY status
   * Archives the old version of model, if exists. Deletes the dataset file.
   *
   * @param model the model
   * @param userName of the model
   * @param version of the model
   * @param modelFile to be set up
   */
  public void activateModel(ModelEntity model, String userName, long version, String modelFile) {
    // ModelEntity modelEntity = modelEntityDAO.find(new ModelId(userName,
    // version));
    // if (modelEntity != null) {
    String dataset = model.getDatasetFile();
    model.setActivated(new Date());
    model.setModelFile(modelFile);
    model.setDatasetFile(null);
    model.setStatus(Status.READY);
    modelEntityDAO.update(model);
    // Archive old version
    if (version != 1L) {
      ModelEntity prevModel = getPreviousModel(model);
      if (prevModel != null) {
        archiveModel(prevModel, userName, version - 1L);
      } else {
        LOG.warn("Cannot archive model (name: {}, version: {}) - the model not found", userName, version - 1L);
      }
    }
    // Delete the dataset file
    if (dataset != null) {
      new File(dataset).delete();
    }
    // } else {
    // LOG.warn("Cannot activate model (name: {}, version: {}) - the model not
    // found", userName, version);
    // }
  }

  /**
   * Archives a model by setting archived date. Deletes old models
   *
   * @param model the model
   * @param userName of the model
   * @param version of the model
   */
  public void archiveModel(ModelEntity model, String userName, long version) {
    // ModelEntity model = modelEntityDAO.find(new ModelId(userName, version));
    // if (model != null) {
    model.setArchived(new Date());
    model.setStatus(Status.ARCHIEVED);
    modelEntityDAO.update(model);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Model (name: " + userName + ", version: " + version + ") archived");
    }
    // Delete old models (version = current version - MAX_STORED_MODELS to be
    // deleted)
    ModelEntity oldModel = getModel(userName, version - MAX_STORED_MODELS);
    if (oldModel != null) {
      deleteModel(oldModel);
    }
    // } else {
    // LOG.warn("Cannot archive model (name: {}, version: {}) - the model not
    // found", userName, version);
    // }
  }

  /**
   * Gets the model for given user and by version.
   *
   * @param userName the user name
   * @param version the version
   * @return the model or null
   */
  public ModelEntity getModel(String userName, long version) {
    return modelEntityDAO.find(new ModelId(userName, version));
  }

  /**
   * Gets the model of latest version.
   *
   * @param userName the user name
   * @return the model or null
   */
  public ModelEntity getLastModel(String userName) {
    return modelEntityDAO.findLastModel(userName);
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
   * Deletes a model from DB and clears its files.
   *
   * @param model the model
   */
  protected void deleteModel(ModelEntity model) {
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Delete old model {}[{}]", model.getName(), model.getVersion());
      }
      if (model.getDatasetFile() != null) {
        new File(model.getDatasetFile()).delete();
      }
      if (model.getModelFile() != null) {
        try {
          // Delete user's folder
          FileUtils.deleteDirectory(new File(model.getModelFile()).getParentFile());
        } catch (IOException e) {
          LOG.warn("Cannot delete model folder: " + model.getModelFile(), e);
        }
      }
    } finally {
      modelEntityDAO.delete(model);
    }
  }

  /**
   * Copies model file to new destination. Only if model has status READY
   * 
   * @param model contains model file to be copied
   * @param dest new folder
   */
  protected void copyModelFile(ModelEntity model, File dest) {
    if (model.getStatus().equals(Status.READY) && model.getModelFile() != null) {
      try {
        FileUtils.copyDirectoryToDirectory(new File(model.getModelFile()), dest);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Old model file copied for {}", model.getName());
        }
      } catch (Throwable e) {
        LOG.error("Failed to copy old model file for " + model.getName(), e);
      }
    }
  }

  /**
   * Submits model for training. If the training fails, it will attempt to train
   * one more time. If will fail again, the model will get FAILED_TRAINING
   * status.
   *
   * @param model the model
   * @param dataset the dataset file
   * @param userName the user name
   */
  public void trainModel(ModelEntity model, File dataset, String userName) {
    // First copy existing model files for incremental training
    // TODO indeed this not always may be desired - we may need train from the
    // scratch also.
    ModelEntity prevModel = getPreviousModel(model);
    if (prevModel != null) {
      copyModelFile(prevModel, dataset.getParentFile());
    }

    // The dataset path to a model in DB
    // TODO Don't use absolute path but use relative to
    // data-collector/datasets path, so it will be possible to relocate
    // file storage w/o modifying DB. Relative path should start from a
    // bucke name.
    model.setDatasetFile(dataset.getAbsolutePath());

    // setProcessing(userName);
    model.setStatus(Status.PROCESSING);
    modelEntityDAO.update(model);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Model {} got status PROCESSING", userName);
    }

    if (!train(model, dataset, userName)) {
      // If the training fails
      // ModelEntity currentModel = getLastModel(userName);
      // if (currentModel != null) {
      // ModelEntity prevModel = getPreviousModel(model);
      if (prevModel != null && Status.READY.equals(prevModel.getStatus())) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Retraining model for {}", userName);
        }
        // Retrain model
        if (!train(model, dataset, userName)) {
          LOG.warn("Model {} failed in retraining. Set status FAILED_TRAINING", userName);
          model.setStatus(Status.FAILED_TRAINING);
          modelEntityDAO.update(model);
        }
      } else {
        LOG.warn("Model {} got status FAILED_TRAINING", userName);
        model.setStatus(Status.FAILED_TRAINING);
        modelEntityDAO.update(model);
      }
      // } else {
      // LOG.warn("Cannot find last model for {}", userName);
      // }
    }
  }

  /**
   * Trains a model in an executor.
   * 
   * @param dataset user dataset
   * @param userName userName
   * @return true if success
   */
  protected boolean train(ModelEntity model, File dataset, String userName) {
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
          if (LOG.isDebugEnabled()) {
            LOG.debug("Model {} successfuly trained", userName);
          }
          // ModelEntity model = getLastModel(userName); // TODO
          activateModel(model, userName, model.getVersion(), modelFolder);
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
   * Sets PROCESSING status to the last model.
   *
   * @param userName a user name
   */
  @Deprecated // TODO not required
  public void setProcessing(String userName) {
    ModelEntity model = getLastModel(userName);
    if (model != null) {
      model.setStatus(Status.PROCESSING);
      modelEntityDAO.update(model);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Model {} got status PROCESSING", userName);
      }
    } else {
      LOG.info("Cannot set PROCESSING status to the model {} - model not found", userName);
    }
  }

  /**
   * Sets RETRY status to the last model if exists
   * 
   * @param userName the model name
   */
  @Deprecated // TODO not used
  public void setRetry(String userName) {
    ModelEntity model = getLastModel(userName);
    if (model != null) {
      model.setStatus(Status.RETRY);
      modelEntityDAO.update(model);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Model {} got status RETRY", userName);
      }
    } else {
      LOG.info("Cannot set RETRY status to the model {} - model not found", userName);
    }

  }

  /**
   * Updates model in database.
   *
   * @param model to be updated
   */
  public void update(ModelEntity model) {
    modelEntityDAO.update(model);
  }

  /**
   * Sets a dataset to the last model
   * 
   * @param userName of model
   * @param dataset to be set up
   */
  @Deprecated // TODO not required
  public void setDatasetToLatestModel(String userName, String dataset) {
    ModelEntity lastModel = getLastModel(userName);
    if (lastModel != null) {
      lastModel.setDatasetFile(dataset);
    }
  }

  /**
   * Gets the previous model of user.
   *
   * @param model the model
   * @return previous model or null
   */
  public ModelEntity getPreviousModel(ModelEntity model) {
    return getModel(model.getName(), model.getVersion() - 1L);
  }
}
