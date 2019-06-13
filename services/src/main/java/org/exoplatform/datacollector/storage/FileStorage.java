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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
     * Return commons work scripts directory, create it if not found.
     *
     * @return the directory file
     */
    protected WorkScriptsDir scriptsDir() {
      return new WorkScriptsDir(this);
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

  public abstract class ScriptsDir extends StorageFile {

    /**
     * 
     */
    private static final long serialVersionUID = -5329453369464173480L;

    protected ScriptsDir(WorkDir parent) {
      super(parent, "scripts".intern());
    }

    protected ScriptsDir(UserDir parent) {
      super(parent, "scripts".intern());
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

    public ScriptFile getTrainingScript() {
      return new ScriptFile(this, "user_feed_train.py");
    }

    public ScriptFile getPredictionScript() {
      return new ScriptFile(this, "user_feed_predict.py");
    }

    public MetadataFile getMetadata() {
      return new MetadataFile(this, "metadata.json");
    }

    public boolean isValid() {
      if (exists() && isDirectory()) {
        try {
          MetadataFile meta = getMetadata();
          // User scripts valid if its metadata contains type and version
          meta.getType();
          meta.getVersion();
          return true;
        } catch (StorageException e) {
          LOG.warn("Cannot read scripts type and version {}", getStoragePath(), e);
        }
      }
      return false;
    }
  }

  public final class WorkScriptsDir extends ScriptsDir {

    /**
     * 
     */
    private static final long serialVersionUID = -2467569739192704393L;

    protected final WorkDir   parent;

    protected WorkScriptsDir(WorkDir parent) {
      super(parent);
      this.parent = parent;
    }

    public WorkDir getWorkDir() {
      return parent;
    }
  }

  public final class ModelScriptsDir extends ScriptsDir {

    /**
     * 
     */
    private static final long serialVersionUID = -7733923664778439691L;

    protected final UserDir   parent;

    protected ModelScriptsDir(UserDir parent) {
      super(parent);
      this.parent = parent;
    }

    public UserDir getUserDir() {
      return parent;
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

    public UserDir getUserDir(String name) throws StorageException {
      UserDir userDir = userDir(name);
      userDir.mkdir();
      if (!userDir.exists()) {
        throw new StorageException("User directory cannot be found: " + userDir.getAbsolutePath());
      }
      initScripts(userDir);
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

  public class MetadataFile extends StorageFile {

    /**
     * 
     */
    private static final long serialVersionUID = 7200362586881200721L;

    private JSONObject        metadata;

    MetadataFile(ScriptsDir parent, String name) {
      super(parent, name);
    }

    private JSONObject metadata() throws StorageException {
      if (metadata == null) {
        JSONParser parser = new JSONParser();
        try (Reader reader = new FileReader(this)) {
          metadata = (JSONObject) parser.parse(reader);
        } catch (ParseException e) {
          throw new StorageException("Error parsing metadata.json of user directory " + this.getStoragePath(), e);
        } catch (IOException e) {
          throw new StorageException("Error reading metadata.json of user directory " + this.getStoragePath(), e);
        }
      }
      return metadata;
    }

    public String getVersion() throws StorageException {
      String version = (String) metadata().get("version");
      if (version == null) {
        throw new StorageException("Value of 'version' cannot be found in metadata.json of scripts directory: "
            + this.getStoragePath());
      }
      return version;
    }

    public String getDescription() throws StorageException {
      String description = (String) metadata().get("description");
      if (description == null) {
        throw new StorageException("Value of 'description' cannot be found in metadata.json of scripts directory: "
            + this.getStoragePath());
      }
      return description;
    }

    public String getType() throws StorageException {
      String type = (String) metadata().get("type");
      if (type == null) {
        throw new StorageException("Value of 'type' cannot be found in metadata.json of scripts directory: "
            + this.getStoragePath());
      }
      return type;
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
      ModelDir modelDir = childDir("model");
      return modelDir;
    }

    public ModelFile getModelDescriptor() {
      return childFile("model.json");
    }

    public ModelFile getUserSnapshot() {
      return childFile("user_snapshot.json");
    }

    public ModelScriptsDir getScriptsDir() {
      return new ModelScriptsDir(this);
    }

    public boolean isValid() {
      return getScriptsDir().isValid();
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

  protected ScriptFile dockerRunScript;

  protected ScriptFile dockerExecScript;

  protected InitParams initParams;

  /**
   * Instantiates a new file storage.
   *
   * @param initParams the init params
   * @throws StorageException the storage exception
   */
  public FileStorage(InitParams initParams) throws StorageException {
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
   * Gets work scripts directory. Work scripts will be used to initialize user
   * directories on first access to them.
   * 
   * @return the work scripts directory
   */
  public WorkScriptsDir getWorkScriptsDir() {
    // TODO It would be reasonable to keep (at least) prediction scripts
    // together with models, as between versions script(s) may change and newer
    // versions may be incompatible. Indeed this also actual for a datasets
    // (format, feature names, targets).
    WorkScriptsDir scriptsDir = getWorkDir().scriptsDir();
    scriptsDir.mkdir();
    if (!scriptsDir.exists()) {
      LOG.warn("Work scripts directory cannot be found: {}", scriptsDir.getAbsolutePath());
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
   * /bucket001/john. If such directory not found or given <code>null</code>,
   * then <code>null</code> will be returned.
   *
   * @param modelPath the model path, can be <code>null</code>, then it will
   *          return <code>null</code>
   * @return the model directory or <code>null</code> if nothing found in the
   *         storage
   */
  public UserDir findUserModel(String modelPath) {
    if (modelPath != null) {
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
   *
   * @throws StorageException the storage exception
   */
  protected void unpackScripts() throws StorageException {
    URL trainingScriptURL = this.getClass().getClassLoader().getResource("scripts/user_feed_train.py");
    URL predictScriptURL = this.getClass().getClassLoader().getResource("scripts/user_feed_predict.py");
    URL datasetutilsURL = this.getClass().getClassLoader().getResource("scripts/datasetutils.py");
    URL dockerRunScriptURL = this.getClass().getClassLoader().getResource("scripts/docker_run.sh");
    URL dockerExecScriptURL = this.getClass().getClassLoader().getResource("scripts/docker_exec.sh");
    WorkScriptsDir scriptsDir = getWorkScriptsDir();
    try {
      ScriptFile trainingScript = scriptsDir.getScript("user_feed_train.py");
      ScriptFile predictionScript = scriptsDir.getScript("user_feed_predict.py");
      ScriptFile datasetutilsScript = scriptsDir.getScript("datasetutils.py");
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
      throw new StorageException("Couldn't unpack training and prediction scripts to work directory "
          + scriptsDir.getAbsolutePath(), e);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Unpacked work scripts to " + scriptsDir.getAbsolutePath());
    }
  }

  protected void initScripts(UserDir userDir) throws StorageException {
    if (userDir.isValid()) {
      return;
    }
    WorkScriptsDir workScripts = getWorkScriptsDir();
    if (workScripts.exists() && workScripts.isDirectory()) {
      try {
        FileUtils.copyDirectory(workScripts, userDir.getScriptsDir());
      } catch (IOException e) {
        throw new StorageException("Cannot init user scripts for " + userDir.getStoragePath()
            + ": error copying work scripts to the user directory", e);
      }
    } else {
      throw new StorageException("Cannot init user scripts " + userDir.getStoragePath() + ": work scripts not found");
    }
  }
}
