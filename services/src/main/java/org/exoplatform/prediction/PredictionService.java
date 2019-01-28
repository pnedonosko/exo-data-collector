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

import org.picocontainer.Startable;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;

/**
 * Service to predict predefined targets on already trained models (see
 * {@link TrainingService}).<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: PredictionService.java 00000 Jan 25, 2019 pnedonosko $
 */
public class PredictionService implements Startable {

  /**
   * Load predictions from a file result on demand.
   */
  protected class LazyPredictFileListAccess implements ListAccess<ExoSocialActivity> {

    protected final String userId;
    
    protected LazyPredictFileListAccess(String userId) {
      this.userId = userId;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ExoSocialActivity[] load(int index, int length) throws Exception, IllegalArgumentException {
      // TODO Do actual prediction here (what will be first load or getSize)
      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSize() throws Exception {
      // TODO Do actual prediction here (what will be first load or getSize)
      return 0;
    }
  }

  protected final TrainingService training;

  /**
   * 
   */
  public PredictionService(TrainingService training) {
    this.training = training;
  }

  public ListAccess<ExoSocialActivity> getUserFeed(Identity userIdentity) {
    return new LazyPredictFileListAccess(userIdentity.getRemoteId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    // TODO any?
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    // TODO any?
  }
}
