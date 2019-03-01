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

  public static final String WORK_DIRECTORY_PARAM   = "work-directory";

  public static final String WORK_DIRECTORY_DEFAULT = "data-collector";

  public static final String FILE_TIMESTAMP_FORMAT  = "yyyy-MM-dd_HHmmss.SSSS";

  protected static String childName(File parent, String name) {
    return new StringBuilder(parent.getName()).append(File.separatorChar).append(name).toString();
  }

  protected static String childName(String parentName, String name) {
    return new StringBuilder(parentName).append(File.separatorChar).append(name).toString();
  }

  public final class WorkDir extends java.io.File {

    /**
     * 
     */
    private static final long serialVersionUID = -4356363994222850622L;

    WorkDir(String localPath) {
      super(localPath);
    }

    /**
     * Return a regular directory, create it if not found.
     *
     * @return the directory file
     */
    protected ScriptsDir scriptsDir() {
      return new ScriptsDir(this);
    }

    /**
     * Data directory.
     *
     * @return the data directory
     */
    protected DataDir dataDir() {
      return new DataDir(this);
    }

    /**
     * Return a regular file. File may or may not to exist.
     *
     * @param name the name
     * @return the file
     */
    protected File childFile(String name) {
      return new File(this, name);
    }

    /**
     * Return a regular directory. Directory may or may not to exist.
     *
     * @param name the name
     * @return the directory
     */
    protected File childDir(String name) {
      File dir = new File(this, name);
      dir.mkdir();
      return dir;
    }
  }

  public abstract class StorageFile extends java.io.File {

    /**
     * 
     */
    private static final long serialVersionUID = 7366628329528836527L;

    protected final File      parent;

    protected final String    storagePath;

    private StorageFile(WorkDir parent, String name) {
      super(parent, name);
      this.parent = parent;
      // FYI, these fields will not be updated if move file!
      this.storagePath = name;
    }

    protected StorageFile(StorageFile parent, String name) {
      super(parent, name);
      this.parent = parent;
      // FYI, these fields will not be updated if move file!
      this.storagePath = childName(parent.getStoragePath(), name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getParentFile() {
      return parent;
    }

    /**
     * Gets the storage path. It's path relative the root of the storage (aka
     * work directory).
     *
     * @return the storage path
     */
    public final String getStoragePath() {
      return storagePath;
    }
  }

  public final class ScriptsDir extends StorageFile {

    /**
     * 
     */
    private static final long serialVersionUID = -5329453369464173480L;

    protected final WorkDir   parent;

    protected ScriptsDir(WorkDir parent) {
      super(parent, "scripts".intern());
      this.parent = parent;
    }

    public WorkDir getWorkDir() {
      return parent;
    }

    /**
     * Get a child file by a name.
     *
     * @param name the name
     * @return the file
     */
    public ScriptFile getScript(String name) {
      return new ScriptFile(this, name);
    }
  }

  public class ScriptFile extends StorageFile {

    /**
     * 
     */
    private static final long  serialVersionUID = 63126458525495498L;

    protected final ScriptsDir parent;

    protected ScriptFile(ScriptsDir parent, String name) {
      super(parent, name);
      this.parent = parent;
    }

    public ScriptsDir getScriptsDir() {
      return parent;
    }
  }

  public final class DataDir extends StorageFile {

    /**
     * 
     */
    private static final long serialVersionUID = -2679190438377054376L;

    protected WorkDir         parent;

    protected DataDir(WorkDir parent) {
      super(parent, "data".intern());
      this.parent = parent;
    }

    /**
     * Gets the work directory where this data folder saved.
     *
     * @return the work directory
     */
    public WorkDir getWorkDir() {
      return parent;
    }

    public BucketDir getBucketDir(String name) {
      BucketDir bucketDir = buketDir(name);
      bucketDir.mkdir();
      if (!bucketDir.exists()) {
        LOG.warn("Bucket directory cannot be found: {}", bucketDir.getAbsolutePath());
      }
      return bucketDir;
    }

    protected BucketDir buketDir(String name) {
      return new BucketDir(this, name);
    }
  }

  public final class BucketDir extends StorageFile {
    /**
     * 
     */
    private static final long serialVersionUID = 3670813842514317016L;

    protected DataDir         parent;

    protected BucketDir(DataDir parent, String name) {
      super(parent, name);
      this.parent = parent;
    }

    /**
     * Gets the data directory where this bucket saved.
     *
     * @return the data directory
     */
    public DataDir getDataDir() {
      return parent;
    }

    public UserDir getUserDir(String name) {
      UserDir userDir = userDir(name);
      userDir.mkdir();
      if (!userDir.exists()) {
        LOG.warn("User directory cannot be found: {}", userDir.getAbsolutePath());
      }
      return userDir;
    }

    protected UserDir userDir(String name) {
      return new UserDir(this, name);
    }
  }

  protected abstract class DataFile extends StorageFile {
    /**
     * 
     */
    private static final long serialVersionUID = -7751434499242337542L;

    private final String      modelPath;

    protected DataFile(BucketDir parent, String name) {
      super(parent, name);
      // FYI, these fields will not be updated if move file!
      this.modelPath = childName(parent.getName(), name);
    }

    protected DataFile(DataFile parent, String name) {
      super(parent, name);
      this.modelPath = parent.getModelPath();
    }

    /**
     * Gets the path of a processing model storage where this file is saved.
     * It's relative path to a storage's data directory, thus it includes a
     * bucket and model name (e.g. /main5/john).
     *
     * @return the model directory path
     */
    public final String getModelPath() {
      return modelPath;
    }
  }

  public class ModelFile extends DataFile {

    /**
     * 
     */
    private static final long serialVersionUID = -4043933204869462822L;

    protected final UserDir   parent;

    protected ModelFile(UserDir parent, String name) {
      super(parent, name);
      this.parent = parent;
    }

    /**
     * Gets the user directory where this user file saved.
     *
     * @return the user directory
     */
    public UserDir getUserDir() {
      return parent;
    }
  }

  public class UserDir extends DataFile {

    /**
     * 
     */
    private static final long serialVersionUID = -2679190438377054376L;

    protected final BucketDir parent;

    protected UserDir(BucketDir parent, String name) {
      super(parent, name);
      this.parent = parent;
    }

    /**
     * Gets the bucket directory where this processing saved.
     *
     * @return the bucket directory
     */
    public BucketDir getBucketDir() {
      return parent;
    }

    public ModelFile getTrainingDataset() {
      return childFile("training.csv");
    }

    public ModelFile getPredictDataset() {
      return childFile("predict.csv");
    }

    public ModelFile getPredictedDataset() {
      return childFile("predicted.csv");
    }

    /**
     * Gets the model processing directory.
     *
     * @return the processing directory
     */
    public ModelDir getModelDir() {
      // TODO need another method/class name, like TFModelDir, or TrainingDir,
      // or ProcessingDir
      ModelDir modelDir = childDir("model");
      return modelDir;
    }

    public ModelFile getModelDescriptor() {
      return childFile("model.json");
    }

    public ModelFile childFile(String name) {
      return new ModelFile(this, name);
    }

    public ModelDir childDir(String name) {
      return new ModelDir(this, name);
    }
  }

  public class ModelDir extends ModelFile {

    /**
     * 
     */
    private static final long serialVersionUID = 4617095370358155195L;

    protected final UserDir   parent;

    protected ModelDir(UserDir parent, String child) {
      super(parent, child);
      this.parent = parent;
    }
  }

  protected WorkDir    workDir;

  protected ScriptFile trainingScript;

  protected ScriptFile predictionScript;

  protected ScriptFile datasetutilsScript;

  protected ScriptFile dockerRunScript;

  protected ScriptFile dockerExecScript;

  protected InitParams initParams;

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
   * 
   * @return the work directory
   */
  public WorkDir getWorkDir() {
    return workDir(true);
  }

  /**
   * Gets the data directory in this storage.
   *
   * @return the data directory
   */
  public DataDir getDataDir() {
    DataDir dataDir = getWorkDir().dataDir();
    dataDir.mkdir();
    if (!dataDir.exists()) {
      LOG.warn("Data directory cannot be found: {}", dataDir.getAbsolutePath());
    }
    return dataDir;
  }

  /**
   * Gets the scripts directory
   * 
   * @return the scripts directory
   */
  public ScriptsDir getScriptsDir() {
    // TODO It would be reasonable to keep (at least) prediction scripts
    // together with models, as between versions script(s) may change and newer
    // versions may be incompatible. Indeed this also actual for a datasets
    // (format, feature names, targets).
    ScriptsDir scriptsDir = getWorkDir().scriptsDir();
    scriptsDir.mkdir();
    if (!scriptsDir.exists()) {
      LOG.warn("Scripts directory cannot be found: {}", scriptsDir.getAbsolutePath());
    }
    return scriptsDir;
  }

  /**
   * Gets the bucket directory.
   *
   * @param bucketName the bucket name, can be <code>null</code> then it will be
   *          generated with 'bucket-$TIMESTAMP' prefix.
   * @return the bucket directory
   */
  public BucketDir getBucketDir(String bucketName) {
    if (bucketName == null || bucketName.trim().length() == 0) {
      bucketName = "bucket-" + new SimpleDateFormat(FILE_TIMESTAMP_FORMAT).format(new Date());
    }
    BucketDir bucketDir = getDataDir().getBucketDir(bucketName);
    return bucketDir;
  }

  /**
   * Gets the training script
   * 
   * @return the training script
   */
  @Deprecated
  public ScriptFile getTrainingScript() {
    return trainingScript;
  }

  /**
   * Gets the prediction script
   * 
   * @return the prediction script
   */
  @Deprecated
  public ScriptFile getPredictionScript() {
    return predictionScript;
  }

  /**
   * Gets the datasetutils script
   * 
   * @return the datasetutils script
   */
  @Deprecated
  public ScriptFile getDatasetutilsScript() {
    return datasetutilsScript;
  }

  /**
   * Gets the dockerRun script
   * 
   * @return the dockerRun script
   */
  @Deprecated
  public ScriptFile getDockerRunScript() {
    return dockerRunScript;
  }

  /**
   * Gets the dockerExec script
   * 
   * @return the dockerRun script
   */
  @Deprecated
  public ScriptFile getDockerExecScript() {
    return dockerExecScript;
  }

  /**
   * Finds the model directory by model path relative to data directory, e.g.
   * /bucket001/john. If such directory not found, then <code>null</code> will
   * be returned.
   *
   * @param modelPath the model path
   * @return the model directory or <code>null</code> if nothing found in the
   *         storage
   */
  public UserDir findUserModel(String modelPath) {
    String[] path = modelPath.split(UserDir.separator);
    if (path.length == 2 && path[0].length() > 0 && path[1].length() > 0) {
      WorkDir workDir = workDir(false);
      if (workDir.exists()) {
        DataDir dataDir = workDir.dataDir();
        if (dataDir.exists()) {
          UserDir userDir = getBucketDir(path[0]).userDir(path[1]);
          if (userDir.exists()) {
            return userDir;
          }
        }
      }
    } else {
      LOG.warn("User directory storage path not correct: {}", modelPath);
    }
    return null;
  }

  protected WorkDir workDir(boolean init) {
    if (this.workDir == null || !this.workDir.exists()) {
      File workDir;
      String workDirPath = getValueParam(WORK_DIRECTORY_PARAM);
      if (workDirPath == null || (workDirPath = workDirPath.trim()).length() == 0) {
        workDirPath = System.getProperty("exo.data.dir");
        if (workDirPath == null || (workDirPath = workDirPath.trim()).length() == 0) {
          workDirPath = System.getProperty("gatein.data.dir");
          if (workDirPath == null || (workDirPath = workDirPath.trim()).length() == 0) {
            workDirPath = System.getProperty("java.io.tmpdir");
            LOG.warn("Platoform data dir not defined. Will use: {}", workDirPath);
          }
        }
        // If use some of predefined destinations, add a work directory inside
        workDir = new File(workDirPath, WORK_DIRECTORY_DEFAULT);
      } else {
        // If configured - use given path as a work directory
        workDir = new File(workDirPath);
      }
      if (init) {
        workDir.mkdirs();
      }
      if (!workDir.exists()) {
        LOG.warn("Working directory cannot be found: {}", workDir.getAbsolutePath());
      }
      this.workDir = new WorkDir(workDir.getPath());
    }
    return this.workDir;
  }

  /**
   * Gets the value of a param in the initParams
   * 
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
   * Unpacks scripts from JAR to scripts directory.
   */
  protected void unpackScripts() {
    URL trainingScriptURL = this.getClass().getClassLoader().getResource("scripts/user_feed_train.py");
    URL predictScriptURL = this.getClass().getClassLoader().getResource("scripts/user_feed_predict.py");
    URL datasetutilsURL = this.getClass().getClassLoader().getResource("scripts/datasetutils.py");
    URL dockerRunScriptURL = this.getClass().getClassLoader().getResource("scripts/docker_run.sh");
    URL dockerExecScriptURL = this.getClass().getClassLoader().getResource("scripts/docker_exec.sh");
    try {
      ScriptsDir scriptsDir = getScriptsDir();
      trainingScript = scriptsDir.getScript("user_feed_train.py");
      predictionScript = scriptsDir.getScript("user_feed_predict.py");
      datasetutilsScript = scriptsDir.getScript("datasetutils.py");
      dockerRunScript = scriptsDir.getScript("docker_run.sh");
      dockerExecScript = scriptsDir.getScript("docker_exec.sh");
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
      LOG.debug("Unpacked training and prediction scripts to: " + getScriptsDir().getAbsolutePath());
    }
  }
}
