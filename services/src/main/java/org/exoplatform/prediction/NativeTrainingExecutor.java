package org.exoplatform.prediction;

import java.io.File;
import java.io.IOException;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NativeTrainingExecutor extends BaseComponentPlugin implements TrainingExecutor {

  /** Logger */
  private static final Log LOG = ExoLogger.getExoLogger(NativeTrainingExecutor.class);

  /**
   * Calls the train script using /bin/sh natively on the host machine.
   * Returns the path of created model folder
   */
  @Override
  public String train(File dataset, String trainingScriptPath) {
    File modelFolder = new File(dataset.getParentFile().getAbsolutePath() + "/model");
    modelFolder.mkdirs();

    String[] cmd = { "python", trainingScriptPath, dataset.getAbsolutePath(), modelFolder.getAbsolutePath() };
    try {
      LOG.info("Running external training script....");
      Process trainingProcess = Runtime.getRuntime().exec(cmd);
      trainingProcess.waitFor();
      return modelFolder.getAbsolutePath();
    } catch (IOException e) {
      LOG.error("Cannot execure external training script: ", e.getMessage());
      return null;
    } catch (InterruptedException e) {
      LOG.warn("Training process has been interrupted");
      return null;
    }

  }

}
