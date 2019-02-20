package org.exoplatform.prediction;

import java.io.File;
import java.io.IOException;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The NativeScriptsExecutor executes scripts natively on the host machine
 */
public class NativeScriptsExecutor extends BaseComponentPlugin implements ModelExecutor {

  public static final String EXEC_ENVIRONMENT_PARAM = "exec-environment";

  /** Logger */
  protected static final Log LOG               = ExoLogger.getExoLogger(NativeScriptsExecutor.class);

  protected final String     execEnv;

  protected FileStorage      fileStorage;

  public NativeScriptsExecutor(FileStorage fileStorage, InitParams initParams) {
    this.fileStorage = fileStorage;
    String execEnv;
    try {
      ValueParam execEnvParam = initParams.getValueParam(EXEC_ENVIRONMENT_PARAM);
      execEnv = execEnvParam.getValue();
    } catch (Exception e) {
      LOG.info("Configuration of {} not set. Scripts will execute directly on the host environment.", EXEC_ENVIRONMENT_PARAM);
      execEnv = null;
    }
    if (execEnv == null || (execEnv = execEnv.trim()).isEmpty()) {
      this.execEnv = null;
    } else {
      this.execEnv = execEnv;
    }
  }

  @Override
  public File train(File dataset) {
    File modelFolder = new File(dataset.getParentFile().getAbsolutePath() + "/model");
    modelFolder.mkdirs();
    executeScript(dataset, fileStorage.getTrainingScript());
    return modelFolder;
  }

  @Override
  public File predict(File dataset) {
    executeScript(dataset, fileStorage.getPredictionScript());
    return new File(dataset.getParentFile(), "predicted.csv");
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
    String[] cmd;
    if (execEnv != null) {
      cmd = new String[] { execEnv, "python", script.getAbsolutePath(), dataset.getAbsolutePath() };
    } else {
      cmd = new String[] { "python", script.getAbsolutePath(), dataset.getAbsolutePath() };
    }
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
