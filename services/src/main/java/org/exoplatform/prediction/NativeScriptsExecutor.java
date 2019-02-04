package org.exoplatform.prediction;

import java.io.File;
import java.io.IOException;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The NativeScriptsExecutor executes scripts natively on the host machine
 *
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
   * @param dataset to be processed
   * @param script to be executed
   */
  protected void executeScript(File dataset, File script) {
    String[] cmd = { "python", script.getAbsolutePath(), dataset.getAbsolutePath() };
    try {
      LOG.info("Running an external script {}...", script.getName());
      Process trainingProcess = Runtime.getRuntime().exec(cmd);
      trainingProcess.waitFor();
    } catch (IOException e) {
      LOG.error("Cannot execute an external script: ", e.getMessage());
    } catch (InterruptedException e) {
      LOG.warn("The script {} execution has been interrupted", script.getName());
    }
  }
}
