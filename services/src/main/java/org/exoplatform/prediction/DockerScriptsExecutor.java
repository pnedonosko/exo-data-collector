package org.exoplatform.prediction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The DockerScriptsExecutor executes scripts in docker containers
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
      LOG.debug(">> Executing docker command {} for {} in {} container", script.getName(), dataset.getName(), containerName);
    }
    String scriptRelativePath = fileStorage.getScriptsDir().getName() + "/" + script.getName();
    // Refactor. It's better to keep relative path to a dataset insdead of
    // absolute one.
    int startIndex = dataset.getAbsolutePath().indexOf(fileStorage.getDatasetsDir().getName());
    String datasetRelativePath = dataset.getAbsolutePath().substring(startIndex);

    String[] cmd = { "/bin/sh", dockerRunScript.getAbsolutePath(), containerName, scriptRelativePath, datasetRelativePath };
    try {
      Process process = Runtime.getRuntime().exec(cmd);
      process.waitFor();
      logDockerOutput(process);
    } catch (Exception e) {
      LOG.warn("Eror occured in the docker container: ", e);
    }
  }

  /**
   * Logs the docker container output
   * 
   * @param process docker process
   */
  protected void logDockerOutput(Process process) {
    if (LOG.isDebugEnabled()) {
      BufferedReader processOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
      Collection<String> allOut = processOut.lines().collect(Collectors.toList());
      if (allOut.size() > 0) {
        LOG.debug("Standard output of docker command:\n");
        for (String s : allOut) {
          LOG.info("> " + s);
        }
      }
    }
    BufferedReader processErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    List<String> allErr = processErr.lines().collect(Collectors.toList());
    if (allErr.size() > 0) {
      LOG.info("Error output of docker command:\n");
      for (String s : allErr) {
        LOG.info("> " + s);
      }
    }
  }
}
