package org.exoplatform.prediction;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.datacollector.storage.FileStorage.ModelFile;
import org.exoplatform.datacollector.storage.FileStorage.ScriptFile;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The NativeScriptsExecutor executes scripts natively on the host machine
 */
public class NativeScriptsExecutor extends FileStorageScriptsExecutor {

  public static final String EXEC_ENVIRONMENT_PARAM = "exec-environment";

  public static final String EXEC_DIRECTORY_PARAM   = "exec-directory";

  /** Logger */
  protected static final Log LOG                    = ExoLogger.getExoLogger(NativeScriptsExecutor.class);

  protected final String     execEnv;

  protected final String     execDir;

  public NativeScriptsExecutor(FileStorage fileStorage, InitParams initParams) {
    super(fileStorage);
    this.execEnv = configParam(initParams, EXEC_ENVIRONMENT_PARAM);
    this.execDir = configParam(initParams, EXEC_DIRECTORY_PARAM);
  }

  /**
   * Executes the script and passes the dataset as an argument
   * 
   * @param dataset to be processed
   * @param script to be executed
   */
  protected void execute(ModelFile dataset, ScriptFile script) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("> Executing command " + script.getName() + " for " + dataset.getModelPath());
    }

    String scriptPath, datasetPath;
    if (execDir != null) {
      scriptPath = new StringBuilder(execDir).append(File.separatorChar).append(script.getStoragePath()).toString();

      datasetPath = new StringBuilder(execDir).append(File.separatorChar).append(dataset.getStoragePath()).toString();
    } else {
      scriptPath = script.getAbsolutePath();
      datasetPath = dataset.getAbsolutePath();
    }

    String[] cmd;
    if (execEnv != null) {
      cmd = new String[] { execEnv, "python", scriptPath, datasetPath };
    } else {
      cmd = new String[] { "python", scriptPath, datasetPath };
    }
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug(">> Running script: {}", Arrays.stream(cmd).collect(Collectors.joining(" ")));
      }
      Process process = Runtime.getRuntime().exec(cmd);
      process.waitFor();
      if (LOG.isDebugEnabled()) {
        LOG.debug("<< Command complete " + script.getName());
      }
    } catch (IOException e) {
      LOG.warn("Error occured while running command " + script.getName() + " for " + dataset.getModelPath(), e);
    } catch (InterruptedException e) {
      LOG.warn("Command execution has been interrupted " + script.getName() + " for " + dataset.getModelPath(), e);
    }
  }
}
