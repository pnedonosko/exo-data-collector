package org.exoplatform.datacollector;

import java.util.TimerTask;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;

/**
 * The Class ContainerCommand.
 */
public abstract class ContainerCommand extends TimerTask {

  /** The container name. */
  final String containerName;

  /**
   * Instantiates a new container command.
   *
   * @param containerName the container name
   */
  ContainerCommand(String containerName) {
    this.containerName = containerName;
  }

  /**
   * Execute actual work of the commend (in extending class).
   *
   * @param exoContainer the exo container
   */
  abstract void execute(ExoContainer exoContainer);

  /**
   * Callback to execute on container error.
   *
   * @param error the error
   */
  abstract void onContainerError(String error);

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