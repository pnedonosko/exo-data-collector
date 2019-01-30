package org.exoplatform.prediction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

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
    executeScript(dataset, fileStorage.getTrainingScript());
    return modelFolder.getAbsolutePath();
  }

  @Override
  public void predict(File dataset) {
    executeScript(dataset, fileStorage.getPredictionScript());
  }

  protected void executeScript(File dataset, File script) {
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
      LOG.info("Running docker container to train the model....");
      Process trainingProcess = Runtime.getRuntime().exec(cmd);
      trainingProcess.waitFor();
      // Only for debugging purposes. Logs all docker output to the console
      logDockerOutput(trainingProcess);

      LOG.info("Container finished working for {}", dataset.getName());
      new File(workDirectory.getAbsolutePath() + "/" + script.getName()).delete();
      new File(workDirectory.getAbsolutePath() + "/" + fileStorage.getDatasetutilsScript().getName()).delete();
      // Compiled python script ends with .pyc.
      new File(workDirectory.getAbsolutePath() + "/" + fileStorage.getDatasetutilsScript().getName() + "c").delete();

    } catch (IOException e) {
      LOG.warn("Error occured while running docker container for {}", dataset.getName());
    } catch (InterruptedException e) {
      LOG.warn("The script {} execution has been interrupted", script.getName());
    }
  }

  protected void logDockerOutput(Process process) {
    try {
      String s = null;
      if (LOG.isDebugEnabled()) {
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        LOG.debug("Standard output of the container:\n");
        while ((s = stdInput.readLine()) != null) {
          LOG.info("> " + s);
        }
      }
      BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      LOG.info("Error output of the container:\n");
      while ((s = stdError.readLine()) != null) {
        LOG.info("> " + s);
      }
    } catch (IOException e) {
      LOG.warn("Cannot log the docker output");
    }
  }
}
