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

import javax.persistence.PersistenceException;

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

  protected ModelExecutor        scriptsExecutor;

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
   * model status: if RETRY that model will be returned instead of creation of a
   * new, If NEW or PROCESSING - cleans up it, if READY - creates a NEW version.
   *
   * @param userName the user name
   * @param dataset the dataset
   * @return the model entity
   */
  public ModelEntity addModel(String userName, String dataset) {
//    ModelEntity lastModel = lastModel(userName);
//    if (lastModel != null && valid(lastModel)) {
//      if (lastModel.getStatus() == Status.NEW || lastModel.getStatus() == Status.PROCESSING) {
//        // FIXME is it correct to delete here? A model added but may be still
//        // collecting will be NEW, but it's correct state and we should return
//        // it instead if dataset the same or raise an exception otherwise.
//        // FYI Feb 13, 2019, in SocialDataCollectorService.submitTraining() we
//        // don't let add a model for NEW or PROCESSING
//        deleteModel(lastModel);
//        LOG.warn("Removed previously added but not complete model {}[{}]", lastModel.getName(), lastModel.getVersion());
//      }
//    }
    ModelEntity newModel = new ModelEntity(userName, dataset);
    modelEntityDAO.create(newModel); // with Status.NEW here
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
   * @param modelFile to be set up
   */
  public void activateModel(ModelEntity model, String modelFile) {
    model.setActivated(new Date());
    model.setModelFile(modelFile);
    model.setDatasetFile(null);
    model.setStatus(Status.READY);
    modelEntityDAO.update(model);
  }

  /**
   * Archives a model by setting archived date. Deletes old models
   *
   * @param model the model
   */
  public void archiveModel(ModelEntity model) {
    model.setArchived(new Date());
    model.setStatus(Status.ARCHIEVED);
    modelEntityDAO.update(model);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Model {}[{}] archived", model.getName(), model.getVersion());
    }
    // Delete old models (version = current version - MAX_STORED_MODELS to be
    // deleted)
    // TODO we may want delete first failed, then READY in the history
    ModelEntity oldModel = getModel(model.getName(), model.getVersion() - MAX_STORED_MODELS);
    if (oldModel != null) {
      deleteModel(oldModel);
    }
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
   * Gets the last model with given status.
   *
   * @param userName the user name
   * @param status the status
   * @return the last model
   */
  public ModelEntity getLastModel(String userName, Status status) {
    return modelEntityDAO.findLastModelWithStatus(userName, status);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    if (scriptsExecutor == null) {
      throw new RuntimeException("ModelExecutor is not configured");
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
        LOG.debug("Delete model {}[{}]", model.getName(), model.getVersion());
      }
      if (model.getDatasetFile() != null) {
        File dataset = new File(model.getDatasetFile());
        if (!dataset.delete()) {
          LOG.warn("Unabled to delete training dataset {}", dataset.getAbsolutePath());
        }
      }
      if (model.getModelFile() != null) {
        File modelFile = new File(model.getModelFile());
        if (modelFile.exists()) {
          try {
            // Delete user's folder
            FileUtils.deleteDirectory(modelFile.getParentFile());
          } catch (IOException e) {
            LOG.warn("Cannot delete model folder: " + model.getModelFile(), e);
          }
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
    if (/* model.getStatus() == Status.READY && */ model.getModelFile() != null) {
      try {
        File modelFile = new File(model.getModelFile());
        if (modelFile.exists()) {
          FileUtils.copyDirectoryToDirectory(modelFile, dest);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Previous model file copied for {}", model.getName());
          }
        } else {
          LOG.warn("Previous model file not found {}", model.getName());
        }
      } catch (Throwable e) {
        LOG.error("Failed to copy previous model file for {}", model.getName(), e);
      }
    }
  }

  /**
   * Submits model for training. If the training fails, it will attempt to train
   * one more time. If will fail again, the model will get FAILED_TRAINING
   * status.
   *
   * @param model the model
   * @param datasetPath the dataset path
   * @param incremental if <code>true</code> an incremental training over
   *          previous model will be attempted
   */
  public void trainModel(ModelEntity model, String datasetPath, boolean incremental) {
    File dataset = new File(datasetPath);
    // We need last READY model to copy it for incremental training
    ModelEntity prevModel = getLastModel(model.getName(), Status.READY); // getPreviousModel(model);
    if (incremental && prevModel != null) {
      // First copy existing model files for incremental training
      copyModelFile(prevModel, dataset.getParentFile());
    }

    // The dataset path to a model in DB
    // TODO Don't use absolute path but use relative to
    // data-collector/datasets path, so it will be possible to relocate
    // file storage w/o modifying DB. Relative path should start from a
    // bucke name.
    model.setDatasetFile(dataset.getAbsolutePath());
    model.setStatus(Status.PROCESSING);
    modelEntityDAO.update(model);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Model {} got status PROCESSING", model.getName());
    }

    boolean success;
    if (!(success = train(model, dataset))) {
      // TODO If the training fails, we try again, but does this is an efficient
      // attempt? Need make a rollback/restore before retrying?
      if (prevModel != null /* && prevModel.getStatus() == Status.READY */) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Retraining model for {}", model.getName());
        }
        // Retrain model
        if (!(success = train(model, dataset))) {
          LOG.warn("Model {} failed in retraining. Set status FAILED_TRAINING", model.getName());
          model.setStatus(Status.FAILED_TRAINING);
          modelEntityDAO.update(model);
        }
      } else {
        LOG.warn("Model {} got status FAILED_TRAINING", model.getName());
        model.setStatus(Status.FAILED_TRAINING);
        modelEntityDAO.update(model);
      }
    }
    if (success) {
      // If training was successful we don't need the dataset anymore
      if (!dataset.delete()) {
        LOG.warn("Unabled to delete training dataset {}", dataset.getAbsolutePath());
      }
      // Archive previous READY version
      if (model.getVersion() != 1L) {
        if (prevModel != null) {
          archiveModel(prevModel);
        } else {
          // TODO we still could roll the history and remove failed models, see
          // also in archiveModel()
          LOG.warn("Cannot archive model {}[{}] - the model not found or not READY", model.getName(), model.getVersion() - 1);
        }
      }
    }
  }

  /**
   * Trains a model in an executor.
   *
   * @param model the model
   * @param dataset user dataset file
   * @return true if success
   */
  protected boolean train(ModelEntity model, File dataset) {
    try {
      // Train model
      File modelFolder = scriptsExecutor.train(dataset);
      // Check if model trained without errors
      File modelFile = new File(dataset.getParentFile() + "/model.json");
      if (modelFile.exists()) {
        JSONParser parser = new JSONParser();
        try {
          JSONObject resultModel = (JSONObject) parser.parse(new FileReader(modelFile));
          String status = (String) resultModel.get("status");
          if (Status.READY.name().equals(status)) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Model {} successfuly trained", model.getName());
            }
            activateModel(model, modelFolder.getAbsolutePath());
            return true;
          }
          String error = (String) resultModel.get("error");
          if (error != null) {
            LOG.error("Model {} training failed. Model error: {}", model.getName(), error);
          } else {
            LOG.error("Model {} training failed. Unexpected model status: {}", model.getName(), status);
          }
        } catch (ParseException e) {
          LOG.error("Model {} training failed. Couldn't parse the model.json file.", model.getName(), e);
        } catch (IOException e) {
          LOG.error("Model {} training failed. Couldn't read the model.json file.", model.getName(), e);
        }
      }
      LOG.warn("The model.json file is not found after training for model {}", model.getName());
    } catch (Exception e) {
      LOG.error("Error trainig {} model on dataset {}", model.getName(), dataset.getPath(), e);
    }
    return false;
  }

  /**
   * Adds a scriptsExecutor plugin. This method is safe in runtime: if
   * configured scriptsExecutor is not an instance of {@link ModelExecutor} then
   * it will log a warning and let server continue the start.
   *
   * @param plugin the plugin
   */
  public void addPlugin(ComponentPlugin plugin) {
    Class<ModelExecutor> pclass = ModelExecutor.class;
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
   * Delete model in database.
   *
   * @param model to be deleted
   */
  public void delete(ModelEntity model) {
    deleteModel(model);
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

  /**
   * Checks if is model valid (has model file). If it is not, the model will be
   * deleted by this method.
   *
   * @param model the model
   * @return <code>true</code>, if is model valid
   */
  public boolean valid(ModelEntity model) {
    if (model.getStatus() == Status.READY || model.getStatus() == Status.ARCHIEVED) {
      String modelFile = model.getModelFile();
      if (modelFile != null) {
        boolean valid = new File(modelFile).exists();
        if (!valid) {
          deleteModel(model);
        }
        return valid;
      }
    } else if (model.getStatus() == Status.PROCESSING) {
      String datasetFile = model.getDatasetFile();
      if (datasetFile != null) {
        return new File(datasetFile).exists();
      }
    } else {
      // else, it's NEW, RETRY or FAILED* - they valid as for the status
      return true;
    }
    return false;
  }

  protected ModelEntity lastModel(String userName) {
    try {
      return getLastModel(userName);
    } catch (PersistenceException e) {
      LOG.error("Error reading last model for {}", userName, e);
      return null;
    }
  }
}
