/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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

import static org.exoplatform.datacollector.ListAccessUtil.BATCH_SIZE;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.picocontainer.Startable;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.datacollector.SocialDataCollectorService;
import org.exoplatform.datacollector.storage.FileStorage.ModelFile;
import org.exoplatform.prediction.model.domain.ModelEntity;
import org.exoplatform.prediction.model.domain.ModelEntity.Status;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.ActivitiesRealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.api.ActivityStorage;

/**
 * Service to predict predefined targets on already trained models (see {@link TrainingService}).<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: PredictionService.java 00000 Jan 25, 2019 pnedonosko $
 */
public class PredictionService implements Startable {

  protected static final Log LOG = ExoLogger.getExoLogger(PredictionService.class);

  protected ModelExecutor    scriptsExecutor;

  /**
   * Load predictions from a file result on demand.
   */
  protected class LazyPredictFileListAccess extends ActivitiesRealtimeListAccess {

    protected final Identity     userIdentity;

    protected final String       modelType;

    protected final List<String> idsOrdered = new ArrayList<>();

    /**
     * Instantiates a new lazy predict file list access.
     *
     * @param userIdentity the user identity
     */
    protected LazyPredictFileListAccess(Identity userIdentity, String modelType) {
      super(activityStorage, ActivityType.ACTIVITY_FEED, userIdentity);
      this.userIdentity = userIdentity;
      this.modelType = modelType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> loadIdsAsList(int index, int limit) {
      // This method is important, it is used by Social's
      // UIUserActivitiesDisplay and we'll use it in Smart Activity Stream

      // We preload first 100 activities, sort them in predicted order and keep
      // in this access list, so each invocation of idsOrdered() will consume
      // it. If loaded 100 activities ends we load next batch and so on.
      // TODO it's not very smart indeed, as need preload all by time period

      int needSize = index + limit;
      if (needSize > idsOrdered.size()) {
        int batchIndex = idsOrdered.size();
        int toRead = needSize - batchIndex;
        int batchLimit = toRead < BATCH_SIZE ? BATCH_SIZE : toRead;
        List<String> nextBatch = predictActivityIdOrder(super.loadIdsAsList(batchIndex, batchLimit));
        idsOrdered.addAll(nextBatch);
      }

      List<String> theList;
      if (idsOrdered.size() == needSize) {
        theList = idsOrdered;
      } else if (idsOrdered.size() > needSize) {
        theList = idsOrdered.subList(index, needSize);
      } else if (idsOrdered.size() > index) {
        theList = idsOrdered.subList(index, idsOrdered.size());
      } else {
        theList = Collections.emptyList();
      }
      // Should it be modifiable - yes!, it will be cast to LinkedList in
      // UIActivitiesContainer.addFirstActivityId() to insert just posted
      // activity - thus we return a copy
      return new LinkedList<String>(theList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> loadAsList(int index, int limit) {
      // TODO do we really need for Smart Activity?
      throw new PredictionNotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> loadNewer(ExoSocialActivity baseActivity, int length) {
      throw new PredictionNotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> loadOlder(ExoSocialActivity baseActivity, int length) {
      throw new PredictionNotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> getUpadtedActivities(Long sinceTime, int limit) {
      throw new PredictionNotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> loadNewer(Long sinceTime, int limit) {
      throw new PredictionNotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> loadOlder(Long sinceTime, int limit) {
      throw new PredictionNotSupportedException("Not implemented");
    }

    // ******* internals *******

    protected List<String> predictActivityIdOrder(List<String> origin) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Activities order before prediction: {}", origin.stream().collect(Collectors.joining(" ")));
      }
      ModelFile dataset;
      if (modelType != null) {
        dataset = collector.collectActivitiesByIds(userIdentity, origin, modelType);
      } else {
        dataset = collector.collectActivitiesByIds(userIdentity, origin);
      }
      if (dataset != null) {
        List<String> ordered = new ArrayList<>();
        try {
          ModelFile predicted = scriptsExecutor.predict(dataset);
          try {
            // We skip a header at first line
            Files.lines(predicted.toPath()).skip(1).forEach(ordered::add);
            if (LOG.isDebugEnabled()) {
              LOG.debug("Activities order after prediction: {}", ordered.stream().collect(Collectors.joining(" ")));
            }
          } catch (IOException e) {
            LOG.error("Cannot read the dataset after prediction", e);
          } finally {
            if (!collector.isDeveloping()) {
              if (!predicted.delete()) {
                LOG.warn("Unabled to delete predicted dataset {}", dataset.getAbsolutePath());
              }
            }
          }
        } finally {
          if (!collector.isDeveloping()) {
            if (!dataset.delete()) {
              LOG.warn("Unabled to delete prediction dataset {}", dataset.getAbsolutePath());
            }
          }
        }
        return ordered;
      } else {
        return Collections.emptyList();
      }
    }
  }

  /** The activityStorage. */
  protected final ActivityStorage            activityStorage;

  protected final TrainingService            training;

  protected final SocialDataCollectorService collector;

  /**
   * Instantiates a new prediction service.
   *
   * @param training the training
   * @param collector the collector
   * @param activityStorage the activity storage
   */
  public PredictionService(TrainingService training, SocialDataCollectorService collector, ActivityStorage activityStorage) {
    this.training = training;
    this.collector = collector;
    this.activityStorage = activityStorage;
  }

  /**
   * Gets the user activity stream feed predictions.
   *
   * @param userIdentity the user identity
   * @return the user stream feed
   */
  public ActivitiesRealtimeListAccess getUserActivityFeed(Identity userIdentity) {
    return new LazyPredictFileListAccess(userIdentity, null);
  }

  /**
   * Gets the user activity stream feed predictions using given model type.
   *
   * @param userIdentity the user identity
   * @param modelType the model type
   * @return the user activity feed
   */
  public ActivitiesRealtimeListAccess getUserActivityFeed(Identity userIdentity, String modelType) {
    return new LazyPredictFileListAccess(userIdentity, modelType);
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
   * Checks if the model exists and has READY status
   * 
   * @param userName of model
   * @return <code>true</code> if the model exists, with READY status and valid files, <code>false</code> otherwise
   */
  public boolean canPredict(String userName) {
    ModelEntity lastModel = lastModel(userName);
    return lastModel != null && lastModel.getStatus() == Status.READY;
  }

  public boolean canPredictBy(String userName, String modelType) {
    try {
      return training.getAvailableModel(userName, modelType) != null;
    } catch (PersistenceException e) {
      LOG.error("Error reading available model of type {} for {}", modelType, userName, e);
    }
    return false;
  }

  /**
   * Adds a scriptsExecutor plugin. This method is safe in runtime: if configured scriptsExecutor is not an instance of
   * {@link ModelExecutor} then it will log a warning and let server continue the start.
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

  protected ModelEntity lastModel(String userName) {
    try {
      return training.getLastModel(userName);
    } catch (PersistenceException e) {
      LOG.error("Error reading last model for {}", userName, e);
    }
    return null;
  }
}
