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

import java.io.InputStream;

import org.picocontainer.Startable;

/**
 * Train ML models using users data and save them in the storage.
 * 
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: TrainingService.java 00000 Dec 14, 2018 pnedonosko $
 */
public class TrainingService implements Startable {

  /**
   * Instantiates a new training service.
   */
  public TrainingService() {
  }
  
  
  /**
   * Submit a new dataset to train a new model in the prediction service.
   *
   * @param data the data stream
   */
  public void addDataset(InputStream data) {
    // TODO
    // 1) Save data to a local file
    // 2) Submit a task to a queue (DB: ModelEntity)
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
