package org.exoplatform.prediction;

import java.io.File;
import java.io.IOException;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NativeTrainingExecutor extends BaseComponentPlugin implements ScriptsExecutor {

  /** Logger */
  protected static final Log LOG = ExoLogger.getExoLogger(NativeTrainingExecutor.class);

  protected FileStorage fileStorage;
  
  public NativeTrainingExecutor(FileStorage fileStorage) {
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

    String[] cmd = { "python", fileStorage.getTrainingScript().getAbsolutePath(), dataset.getAbsolutePath(), modelFolder.getAbsolutePath() };
    try {
      LOG.info("Running external training script....");
      Process trainingProcess = Runtime.getRuntime().exec(cmd);
      trainingProcess.waitFor();
      return modelFolder.getAbsolutePath();
    } catch (IOException e) {
      LOG.error("Cannot execute external training script: ", e.getMessage());
      return null;
    } catch (InterruptedException e) {
      LOG.warn("Training process has been interrupted");
      return null;
    }
  }
  @Override
  public void predict(File dataset) {
    // TODO Auto-generated method stub
    
  }
  
}
