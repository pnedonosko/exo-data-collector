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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.picocontainer.Startable;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.datacollector.SocialDataCollectorService;
import org.exoplatform.prediction.model.domain.ModelEntity;
import org.exoplatform.prediction.model.domain.ModelEntity.Status;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.ActivitiesRealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.api.ActivityStorage;

/**
 * Service to predict predefined targets on already trained models (see
 * {@link TrainingService}).<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: PredictionService.java 00000 Jan 25, 2019 pnedonosko $
 */
public class PredictionService implements Startable {

  public static final int    MAX_BATCH_SIZE = 100;

  protected static final Log LOG            = ExoLogger.getExoLogger(PredictionService.class);

  protected ScriptsExecutor  scriptsExecutor;

  /**
   * Load predictions from a file result on demand.
   */
  protected class LazyPredictFileListAccess extends ActivitiesRealtimeListAccess {

    protected final Identity userIdentity;

    protected List<String>   loadIdsAsList;

    protected int            loadIdsAsListIndex = 0;

    /**
     * Instantiates a new lazy predict file list access.
     *
     * @param userIdentity the user identity
     */
    protected LazyPredictFileListAccess(Identity userIdentity) {
      super(activityStorage, ActivityType.ACTIVITY_FEED, userIdentity);
      this.userIdentity = userIdentity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> loadIdsAsList(int index, int limit) {
      // TODO this method is important, it is used by Social's
      // UIUserActivitiesDisplay and we'll use it in Smart Activity Stream

      // We preload first 100 activities, sort them in predicted order and keep
      // in this access list, so each invocation of loadIdsAsList() will consume
      // it. If loaded 100 activities ends we return empty list.
      // TODO it's not very smart indeed, as need preload all by time period

      if (loadIdsAsList != null && (index + limit) > loadIdsAsList.size()) {
        loadIdsAsList = null;
        loadIdsAsListIndex = index;
      }

      if (loadIdsAsList == null) {
        int feedLimit = limit < MAX_BATCH_SIZE ? MAX_BATCH_SIZE : limit;
        loadIdsAsList = predictActivityIdOrder(super.loadIdsAsList(loadIdsAsListIndex, feedLimit));
      }

      List<String> batch = loadIdsAsList.subList(index, limit);
      return Collections.unmodifiableList(batch); // should it be modifiable?
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> loadAsList(int index, int limit) {
      // TODO do we really need for Smart Activity?
      // return super.loadAsList(index, limit);
      throw new PredictionNotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> loadNewer(ExoSocialActivity baseActivity, int length) {
      // return super.loadNewer(baseActivity, length);
      throw new PredictionNotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> loadOlder(ExoSocialActivity baseActivity, int length) {
      // return super.loadOlder(baseActivity, length);
      throw new PredictionNotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> getUpadtedActivities(Long sinceTime, int limit) {
      // return super.getUpadtedActivities(sinceTime, limit);
      throw new PredictionNotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> loadNewer(Long sinceTime, int limit) {
      // return super.loadNewer(sinceTime, limit);
      throw new PredictionNotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExoSocialActivity> loadOlder(Long sinceTime, int limit) {
      // return super.loadOlder(sinceTime, limit);
      throw new PredictionNotSupportedException("Not implemented");
    }

    // ******* internals *******

    protected List<String> predictActivityIdOrder(List<String> origin) {
      // TODO do this on demand in list access
      LOG.info("ORIGIN: ");
      origin.forEach(LOG::info);
      File dataset = new File(collector.collectUserFeed(userIdentity.getRemoteId()));
      scriptsExecutor.predict(dataset);
      List<String> ordered = new ArrayList<>();
      try {
        Files.lines(dataset.toPath()).skip(1).forEach(ordered::add);
      } catch (IOException e) {
        LOG.warn("Cannot read the dataset after prediction.");
      }

      LOG.info("ORDERED: ");
      ordered.forEach(LOG::info);
      return ordered;
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
    // TODO:
    // 1) load the updated data (CSV) and order the user feed according the rank
    // 2) return ordered feed as LazyPredictFileListAccess
    return new LazyPredictFileListAccess(userIdentity);
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
    // TODO any?
  }

  /**
   * Checks if the model exists and has READY status
   * @param userName of model
   * @return true if the model exists and has READY status, false otherwise
   */
  public boolean canPredict(String userName) {
    ModelEntity model = training.getLastModel(userName);
    return model != null && Status.READY.equals(model.getStatus());
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
}
