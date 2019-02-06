package org.exoplatform.prediction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The DockerScriptsExecutor executes scripts in docker containers
 */
public class DockerScriptsExecutor extends BaseComponentPlugin implements ScriptsExecutor {

  protected static final Log  LOG = ExoLogger.getExoLogger(DockerScriptsExecutor.class);

  protected final FileStorage fileStorage;

  protected final File        dockerRunScript;

  public DockerScriptsExecutor(FileStorage fileStorage) {
    this.fileStorage = fileStorage;
    this.dockerRunScript = fileStorage.getDockerRunScript();
  }

  @Override
  public String train(File dataset) {
    File modelFolder = new File(dataset.getParentFile().getAbsolutePath() + "/model");
    modelFolder.mkdirs();
    executeCommand(dataset, fileStorage.getTrainingScript());
    return modelFolder.getAbsolutePath();
  }

  @Override
  public String predict(File dataset) {
    executeCommand(dataset, fileStorage.getPredictionScript());
    return dataset.getAbsolutePath().replace(dataset.getName(), "predicted.csv");
  }

  /**
   * Executes given command script with the dataset as an argument.
   * 
   * @param dataset to be processed
   * @param script to be executed
   */
  protected void executeCommand(File dataset, File script) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(">> Executing docker command " + script.getName() + " for " + dataset.getName());
    }

    // The folder of a dataset is the work directory for docker
    File workDirectory = dataset.getParentFile();
    try {
      // Copy scripts to the docker work directory
      FileUtils.copyFileToDirectory(script, workDirectory);
      FileUtils.copyFileToDirectory(fileStorage.getDatasetutilsScript(), workDirectory);
    } catch (IOException e) {
      LOG.error("Cannot copy scripts to the work directory {}, {}", workDirectory.getPath(), e.getMessage());
    }

    String[] cmd = { "/bin/sh", dockerRunScript.getAbsolutePath(), workDirectory.getAbsolutePath(), script.getName(),
        dataset.getName() };
    try {
      Process process = Runtime.getRuntime().exec(cmd);
      process.waitFor();
      // Only for debugging purposes. Logs all docker output to the console
      logDockerOutput(process);

      new File(workDirectory.getAbsolutePath() + "/" + script.getName()).delete();
      new File(workDirectory.getAbsolutePath() + "/" + fileStorage.getDatasetutilsScript().getName()).delete();
      // Compiled python script ends with .pyc.
      new File(workDirectory.getAbsolutePath() + "/" + fileStorage.getDatasetutilsScript().getName() + "c").delete();

      if (LOG.isDebugEnabled()) {
        LOG.debug("<< Docker command complete " + script.getName());
      }
    } catch (IOException e) {
      LOG.warn("Error occured while running docker command " + script.getName() + " for " + dataset.getName(), e);
    } catch (InterruptedException e) {
      LOG.warn("Docker command execution has been interrupted " + script.getName() + " for " + dataset.getName(), e);
    }
  }

  /**
   * Logs the docker container output
   * 
   * @param process docker process
   */
  protected void logDockerOutput(Process process) {
    try {
      if (LOG.isDebugEnabled()) {
        BufferedReader processOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Collection<String> allOut = readAll(processOut);
        if (allOut.size() > 0) {
          LOG.debug("Standard output of docker command:\n");
          for (String s : allOut) {
            LOG.info("> " + s);
          }
        }
      }
      BufferedReader processErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      Collection<String> allErr = readAll(processErr);
      if (allErr.size() > 0) {
        LOG.info("Error output of docker command:\n");
        for (String s : allErr) {
          LOG.info("> " + s);
        }
      }
    } catch (IOException e) {
      LOG.error("Cannot read docker command output", e);
    }
  }

  private Collection<String> readAll(BufferedReader input) throws IOException {
    String s = null;
    List<String> res = new ArrayList<>();
    while ((s = input.readLine()) != null) {
      res.add(s);
    }
    return res;
  }
}
