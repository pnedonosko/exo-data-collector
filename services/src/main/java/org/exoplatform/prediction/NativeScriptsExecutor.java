package org.exoplatform.prediction;

import java.io.File;
import java.io.IOException;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The NativeScriptsExecutor executes scripts natively on the host machine
 */
public class NativeScriptsExecutor extends BaseComponentPlugin implements ScriptsExecutor {

  /** Logger */
  protected static final Log LOG = ExoLogger.getExoLogger(NativeScriptsExecutor.class);

  protected FileStorage      fileStorage;

  public NativeScriptsExecutor(FileStorage fileStorage) {
    this.fileStorage = fileStorage;
  }

  @Override
  public String train(File dataset) {
    File modelFolder = new File(dataset.getParentFile().getAbsolutePath() + "/model");
    modelFolder.mkdirs();
    executeScript(dataset, fileStorage.getTrainingScript());
    return modelFolder.getAbsolutePath();
  }

  @Override
  public String predict(File dataset) {
    executeScript(dataset, fileStorage.getPredictionScript());
    return dataset.getAbsolutePath().replace(dataset.getName(), "predicted.csv");
  }

  /**
   * Executes the script and passes the dataset as an argument
   * 
   * @param dataset to be processed
   * @param script to be executed
   */
  protected void executeScript(File dataset, File script) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> Executing command " + script.getName() + " for " + dataset.getName());
    }
    String[] cmd = { "python", script.getAbsolutePath(), dataset.getAbsolutePath() };
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Running an external script {}...", script.getName());
      }
      Process process = Runtime.getRuntime().exec(cmd);
      process.waitFor();
      if (LOG.isDebugEnabled()) {
        LOG.debug("<< Command complete " + script.getName());
      }
    } catch (IOException e) {
      LOG.warn("Error occured while running command " + script.getName() + " for " + dataset.getName(), e);
    } catch (InterruptedException e) {
      LOG.warn("Command execution has been interrupted " + script.getName() + " for " + dataset.getName(), e);
    }
  }
}
