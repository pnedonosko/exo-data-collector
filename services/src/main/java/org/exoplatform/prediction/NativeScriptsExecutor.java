package org.exoplatform.prediction;

import java.io.File;
import java.io.IOException;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NativeScriptsExecutor extends BaseComponentPlugin implements ScriptsExecutor {

  /** Logger */
  protected static final Log LOG = ExoLogger.getExoLogger(NativeScriptsExecutor.class);

  protected FileStorage      fileStorage;

  public NativeScriptsExecutor(FileStorage fileStorage) {
    this.fileStorage = fileStorage;
  }

  /**
   * Calls the train script using /bin/sh natively on the host machine.
   * Returns the path of created model folder
   */
  @Override
  public String train(File dataset) {
    File modelFolder = new File(dataset.getParentFile().getAbsolutePath() + "/model");
    modelFolder.mkdirs();
    executeScript(dataset, fileStorage.getTrainingScript());
    return modelFolder.getAbsolutePath();
  }

  @Override
  public void predict(File dataset) {
    executeScript(dataset, fileStorage.getPredictionScript());
  }

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
