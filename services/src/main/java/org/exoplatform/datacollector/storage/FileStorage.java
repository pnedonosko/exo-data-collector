/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.datacollector.storage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Common tools for accessing file system storage. <br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: FileStorage.java 00000 Jan 28, 2019 pnedonosko $
 */
public class FileStorage {

  private static final Log   LOG                    = ExoLogger.getExoLogger(FileStorage.class);

  public static final String WORK_DIRECTORY_PARAM   = "work-directorty";

  public static final String WORK_DIRECTORY_DEFAULT = "data-collector";

  public static final String FILE_TIMESTAMP_FORMAT  = "yyyy-MM-dd_HHmmss.SSSS";

  protected File             trainingScript;

  protected File             predictionScript;

  protected File             datasetutilsScript;

  protected File             dockerRunScript;
  
  protected File             dockerExecScript;

  protected InitParams       initParams;

  /**
   * Instantiates a new file storage.
   *
   * @param initParams the init params
   */
  public FileStorage(InitParams initParams) {
    this.initParams = initParams;
    unpackScripts();
  }

  /**
   * Gets the work directory
   * @return the work directory
   */
  public File getWorkDir() {
    String workDirPath = getValueParam(WORK_DIRECTORY_PARAM);
    File workDir;
    if (workDirPath == null) {
      // XXX Use temp dir in development, then switch to eXo data dir.
      workDirPath = System.getProperty("java.io.tmpdir");
      if (workDirPath == null || workDirPath.trim().length() == 0) {
        // dataDirPath = System.getProperty("gatein.data.dir");
        workDirPath = System.getProperty("exo.data.dir");
        if (workDirPath == null || workDirPath.trim().length() == 0) {
          workDirPath = System.getProperty("java.io.tmpdir");
          LOG.warn("Platoform data dir not defined. Will use: {}", workDirPath);
        }
      }
      workDir = new File(workDirPath, WORK_DIRECTORY_DEFAULT);
    } else {
      workDir = new File(workDirPath);
    }
    workDir.mkdirs();

    return workDir;
  }

  /**
   * Gets the datasets directory
   * @return the datasets directory
   */
  public File getDatasetsDir() {
    File datasetsDir = new File(getWorkDir(), "datasets");
    datasetsDir.mkdirs();

    return datasetsDir;
  }

  /**
   * Gets the scripts directory
   * @return the scripts directory
   */
  public File getScriptsDir() {
    // TODO It would be reasonable to keep (at least) prediction scripts
    // together with models, as between versions script(s) may change and newer
    // versions may be incompatible. Indeed this also actual for a datasets
    // (format, feature names, targets).
    File scriptsDir = new File(getWorkDir(), "scripts");
    scriptsDir.mkdirs();

    return scriptsDir;
  }

  /**
   * Gets the bucket directory
   * @return the bucket directory
   */
  public File getBucketDir(String bucketName) {
    // TODO Spool all users datasets into a dedicated folder into
    // $WORK_DIR/datasets/${bucketName}

    File datasetsDir = getDatasetsDir();

    if (bucketName == null || bucketName.trim().length() == 0) {
      bucketName = "bucket-" + new SimpleDateFormat(FILE_TIMESTAMP_FORMAT).format(new Date());
    }

    File bucketDir = new File(datasetsDir, bucketName);
    bucketDir.mkdirs();

    return bucketDir;
  }

  /**
   * Gets the training script
   * @return the training script
   */
  public File getTrainingScript() {
    return trainingScript;
  }

  /**
   * Gets the prediction script
   * @return the prediction script
   */
  public File getPredictionScript() {
    return predictionScript;
  }

  /**
   * Gets the datasetutils script
   * @return the datasetutils script
   */
  public File getDatasetutilsScript() {
    return datasetutilsScript;
  }

  /**
   * Gets the dockerRun script
   * @return the dockerRun script
   */
  public File getDockerRunScript() {
    return dockerRunScript;
  }

  /**
   * Gets the dockerExec script
   * @return the dockerRun script
   */
  public File getDockerExecScript() {
    return dockerExecScript;
  }
  /**
   * Gets the value of a param in the initParams
   * @param keyName of the param
   * @return the value
   */
  protected String getValueParam(String keyName) {
    if (initParams != null) {
      ValueParam param = initParams.getValueParam(keyName);
      try {
        if (param != null) {
          return param.getValue();
        }
      } catch (Exception e) {
        LOG.warn("Cannot read value param {}", keyName, e);
      }
    }
    return null;
  }

  /**
   * Unpacks scripts from JAR to scripts directory
   */
  protected void unpackScripts() {
    URL trainingScriptURL = this.getClass().getClassLoader().getResource("scripts/user_feed_train.py");
    URL predictScriptURL = this.getClass().getClassLoader().getResource("scripts/user_feed_predict.py");
    URL datasetutilsURL = this.getClass().getClassLoader().getResource("scripts/datasetutils.py");
    URL dockerRunScriptURL = this.getClass().getClassLoader().getResource("scripts/docker_run.sh");
    URL dockerExecScriptURL = this.getClass().getClassLoader().getResource("scripts/docker_exec.sh");
    try {
      File scriptsDir = getScriptsDir();
      scriptsDir.mkdirs();
      trainingScript = new File(scriptsDir, "user_feed_train.py");
      predictionScript = new File(scriptsDir, "user_feed_predict.py");
      datasetutilsScript = new File(scriptsDir, "datasetutils.py");
      dockerRunScript = new File(scriptsDir, "docker_run.sh");
      dockerExecScript = new File(scriptsDir, "docker_exec.sh");
      FileUtils.copyURLToFile(trainingScriptURL, trainingScript);
      FileUtils.copyURLToFile(predictScriptURL, predictionScript);
      FileUtils.copyURLToFile(datasetutilsURL, datasetutilsScript);
      FileUtils.copyURLToFile(dockerRunScriptURL, dockerRunScript);
      FileUtils.copyURLToFile(dockerExecScriptURL, dockerExecScript);
      trainingScript.deleteOnExit();
      predictionScript.deleteOnExit();
      datasetutilsScript.deleteOnExit();
      dockerRunScript.deleteOnExit();
      dockerExecScript.deleteOnExit();
      scriptsDir.deleteOnExit();
    } catch (IOException e) {
      LOG.error("Couldn't unpack training and prediction scripts", e);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Unpacked training and prediction scripts to: " + getScriptsDir());
    }
  }

}
