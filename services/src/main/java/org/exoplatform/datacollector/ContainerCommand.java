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
package org.exoplatform.datacollector;

import java.util.TimerTask;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The Class ContainerCommand.
 */
public abstract class ContainerCommand extends TimerTask {

  private static final Log LOG = ExoLogger.getExoLogger(ContainerCommand.class);

  /** The container name. */
  protected final String             containerName;

  /**
   * Instantiates a new container command.
   *
   * @param containerName the container name
   */
  protected ContainerCommand(String containerName) {
    this.containerName = containerName;
  }

  /**
   * Instantiates a new command in current container.
   */
  protected ContainerCommand() {
    this(ExoContainerContext.getCurrentContainer().getContext().getName());
  }

  /**
   * Execute actual work of the commend (in extending class).
   *
   * @param exoContainer the exo container
   */
  protected abstract void execute(ExoContainer exoContainer);

  /**
   * Callback to execute on container error.
   *
   * @param error the error
   */
  protected void onContainerError(String error) {
    LOG.error("Cannot run command in {} container: {}", containerName, error);
  }
  
  /**
   * Callback to execute on execution error.
   *
   * @param error the error
   */
  protected void onError(Throwable error) {
    LOG.error("Error running command in {}", containerName, error);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    // Do the work under eXo container context (for proper work of eXo apps
    // and JPA storage)
    ExoContainer exoContainer = ExoContainerContext.getContainerByName(containerName);
    if (exoContainer != null) {
      ExoContainer contextContainer = ExoContainerContext.getCurrentContainerIfPresent();
      try {
        // Container context
        ExoContainerContext.setCurrentContainer(exoContainer);
        RequestLifeCycle.begin(exoContainer);
        // do the work here
        execute(exoContainer);
      } catch (Throwable e) {
        onError(e);
      } finally {
        // Restore context
        RequestLifeCycle.end();
        ExoContainerContext.setCurrentContainer(contextContainer);
      }
    } else {
      onContainerError("Container not found");
    }
  }
}
