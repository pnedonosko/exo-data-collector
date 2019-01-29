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
import java.text.SimpleDateFormat;
import java.util.Date;

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

  protected final InitParams initParams;

  /**
   * Instantiates a new file storage.
   *
   * @param initParams the init params
   */
  public FileStorage(InitParams initParams) {
    this.initParams = initParams;
  }

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

  public File getDatasetsDir() {
    File datasetsDir = new File(getWorkDir(), "datasets");
    datasetsDir.mkdirs();

    return datasetsDir;
  }

  public File getScriptsDir() {
    // TODO It would be reasonable to keep (at least) prediction scripts
    // together with models, as between versions script(s) may change and newer
    // versions may be incompatible. Indeed this also actual for a datasets
    // (format, feature names, targets).
    File datasetsDir = new File(getWorkDir(), "scripts");
    datasetsDir.mkdirs();

    return datasetsDir;
  }

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

}
