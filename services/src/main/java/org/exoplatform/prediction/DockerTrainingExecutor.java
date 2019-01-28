package org.exoplatform.prediction;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class DockerTrainingExecutor extends BaseComponentPlugin implements TrainingExecutor {

  private static final Log LOG = ExoLogger.getExoLogger(DockerTrainingExecutor.class);;

  @Override
  public String train(File dataset, String trainingScriptPath) {
    File scriptFile = new File(trainingScriptPath);
    // The docker .sh script
    String dockerScriptPath = scriptFile.getParentFile().getAbsolutePath() + "/docker_train.sh";
    // The folder of training script is the work directory for docker
    File workDirectory = dataset.getParentFile();
    File modelFolder = new File(workDirectory.getAbsolutePath() + "/model");
    modelFolder.mkdirs();
    try {
      // Copy scripts to the docker work directory
      FileUtils.copyFileToDirectory(scriptFile, workDirectory);
      // TODO: refactor
      FileUtils.copyFileToDirectory(new File(scriptFile.getParentFile().getAbsolutePath() + "/datasetutils.py"), workDirectory);
    } catch (IOException e) {
      LOG.error("Cannot copy dataset {} to the work directory, {}", dataset.getName(), e.getMessage());
      return null;
    }
    String[] cmd = { "/bin/sh", dockerScriptPath, workDirectory.getAbsolutePath(), scriptFile.getName(), dataset.getName() };
    try {
      LOG.info("Running docker container to train the model....");
      Process trainingProcess = Runtime.getRuntime().exec(cmd);
      trainingProcess.waitFor();

      // Only for debugging purposes. Logs all docker output to the console
      /*
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(trainingProcess.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(trainingProcess.getErrorStream()));
      LOG.info("Here is the standard output of the container:\n");
      String s = null;
      while ((s = stdInput.readLine()) != null) {
        LOG.info(s);
      }
      LOG.info("Here is the error output of the container:\n");
      while ((s = stdError.readLine()) != null) {
        LOG.info(s);
      }*/

      LOG.info("Container finished working for {}", dataset.getName());
      new File(workDirectory.getAbsolutePath() + "/" + scriptFile.getName()).delete();
      new File(workDirectory.getAbsolutePath() + "/datasetutils.py").delete();
      new File(workDirectory.getAbsolutePath() + "/datasetutils.pyc").delete();

      return dataset.getParentFile().getAbsolutePath() + "/model";
    } catch (IOException e) {
      LOG.warn("Error occured while running docker container for {}", dataset.getName());
      return null;
    } catch (InterruptedException e) {
      LOG.warn("Training process has been interrupted");
      return null;
    }
  }

}
