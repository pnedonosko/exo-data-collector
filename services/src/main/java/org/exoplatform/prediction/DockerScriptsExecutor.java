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
package org.exoplatform.prediction;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.datacollector.storage.FileStorage.ModelFile;
import org.exoplatform.datacollector.storage.FileStorage.ScriptFile;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The DockerScriptsExecutor executes scripts in docker containers
 */
public class DockerScriptsExecutor extends FileStorageScriptsExecutor {

  public static final String EXEC_CONTAINER_NAME_PARAM = "exec-container-name";

  protected static final Log LOG                       = ExoLogger.getExoLogger(DockerScriptsExecutor.class);

  protected final String     dockerScriptPath;

  protected final String     execContainerName;

  public DockerScriptsExecutor(FileStorage fileStorage, InitParams initParams) {
    super(fileStorage);
    String execContainerName = configParam(initParams, EXEC_CONTAINER_NAME_PARAM);
    if (execContainerName == null || execContainerName.trim().isEmpty()) {
      this.execContainerName = null;
      this.dockerScriptPath = fileStorage.getDockerRunScript().getAbsolutePath();
    } else {
      this.execContainerName = execContainerName;
      this.dockerScriptPath = fileStorage.getDockerExecScript().getAbsolutePath();
    }
  }

  /**
   * Executes given command script with the dataset as an argument.
   * 
   * @param dataset to be processed
   * @param script to be executed
   */
  protected void execute(ModelFile dataset, ScriptFile script) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("> Executing Docker command {} for {}", script.getName(), dataset.getModelPath());
    }
    String scriptPath = script.getStoragePath();
    String datasetPath = dataset.getStoragePath();
    String[] cmd;
    if (execContainerName != null) {
      cmd = new String[] { "/bin/sh", dockerScriptPath, execContainerName, scriptPath, datasetPath };
    } else {
      cmd = new String[] { "/bin/sh", dockerScriptPath, fileStorage.getWorkDir().getAbsolutePath(), scriptPath, datasetPath };
    }
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug(">> Running Docker script: {}", Arrays.stream(cmd).collect(Collectors.joining(" ")));
      }
      Process process = Runtime.getRuntime().exec(cmd);
      process.waitFor();
      logOutput(process, LOG);
      if (LOG.isDebugEnabled()) {
        LOG.debug("<< Docker command complete: " + script.getName());
      }
    } catch (Exception e) {
      LOG.error("Docker command {} failed for {}", script.getName(), dataset.getModelPath(), e);
    }
  }
}
