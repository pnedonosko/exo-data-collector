package org.exoplatform.prediction;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class DockerTrainingExecutor extends BaseComponentPlugin implements TrainingExecutor {

  private static final Log LOG = ExoLogger.getExoLogger(DockerTrainingExecutor.class);

  @Override
  public String train(File dataset, String trainingScriptPath) {
    File scriptFile = new File(trainingScriptPath);
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

    String[] cmd = { "docker", "run", "-v", workDirectory.getAbsolutePath() + ":/tmp", "-w", "/tmp", "tensorflow/tensorflow",
        "python", scriptFile.getName(), dataset.getName() };

    try {
      LOG.info("Running docker container to train the model....");

      Process trainingProcess = Runtime.getRuntime().exec(cmd);
      trainingProcess.waitFor();

      LOG.info("Container finished working for {}. Starting deleting tmp", dataset.getName());
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
