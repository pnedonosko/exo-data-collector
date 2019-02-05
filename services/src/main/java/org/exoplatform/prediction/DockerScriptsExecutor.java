package org.exoplatform.prediction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The DockerScriptsExecutor executes scripts in docker containers
 *
 */
public class DockerScriptsExecutor extends BaseComponentPlugin implements ScriptsExecutor {

  protected static final Log    LOG                  = ExoLogger.getExoLogger(DockerScriptsExecutor.class);

  protected static final String CONTAINER_NAME_PARAM = "container-name";

  protected final FileStorage   fileStorage;

  protected final File          dockerRunScript;

  protected final String        containerName;

  public DockerScriptsExecutor(FileStorage fileStorage, InitParams initParams) {
    this.fileStorage = fileStorage;
    this.dockerRunScript = fileStorage.getDockerRunScript();

    ValueParam containerNameParam = initParams.getValueParam(CONTAINER_NAME_PARAM);
    this.containerName = containerNameParam.getValue();
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
    String scriptRelativePath = fileStorage.getScriptsDir().getName() + "/" + script.getName();
    // Refactor. It's better to keep relative path to a dataset insdead of
    // absolute one.
    int startIndex = dataset.getAbsolutePath().indexOf(fileStorage.getDatasetsDir().getName());
    String datasetRelativePath = dataset.getAbsolutePath().substring(startIndex);

    String[] cmd = { "/bin/sh", dockerRunScript.getAbsolutePath(), containerName, scriptRelativePath,
        datasetRelativePath };
    LOG.info("Running {} in the container {} for dataset {}", script.getName(), containerName, dataset.getName());
    try {
      Process process = Runtime.getRuntime().exec(cmd);
      process.waitFor();
      logDockerOutput(process);
    } catch (Exception e) {
      LOG.warn("Eror occured in the docker container.", e);
    }
    LOG.info("Container finished working for the model");
  }

  /**
   * Logs the docker container output
   * @param process docker process
   */
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
