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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.datacollector.storage.FileStorage;
import org.exoplatform.datacollector.storage.FileStorage.ModelDir;
import org.exoplatform.datacollector.storage.FileStorage.ModelFile;
import org.exoplatform.datacollector.storage.FileStorage.ScriptFile;
import org.exoplatform.services.log.Log;

/**
 * Abstract FileStorageScriptsExecutor to executes scripts from {@link FileStorage}.
 */
public abstract class FileStorageScriptsExecutor extends BaseComponentPlugin implements ModelExecutor {

  protected FileStorage fileStorage;

  public FileStorageScriptsExecutor(FileStorage fileStorage) {
    this.fileStorage = fileStorage;
  }

  @Override
  public ModelDir train(ModelFile dataset) throws ModelExecutorException {
    execute(dataset, dataset.getUserDir().getScriptsDir().getTrainingScript()); // fileStorage.getTrainingScript()
    return dataset.getUserDir().getModelDir();
  }

  @Override
  public ModelFile predict(ModelFile dataset) throws ModelExecutorException {
    execute(dataset, dataset.getUserDir().getScriptsDir().getPredictionScript()); // fileStorage.getPredictionScript());
    return dataset.getUserDir().getPredictedDataset();
  }

  protected String configParam(InitParams initParams, String key) {
    ValueParam execEnvParam = initParams.getValueParam(key);
    String val = execEnvParam != null ? execEnvParam.getValue() : null;
    if (val == null || (val = val.trim()).isEmpty()) {
      return null;
    } else {
      return val;
    }
  }

  /**
   * Logs the process output and error.
   *
   * @param process the process
   * @param log the log
   */
  protected void logOutput(Process process, Log log) throws ModelExecutorException {
    if (log.isDebugEnabled()) {
      BufferedReader processOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
      Collection<String> allOut = processOut.lines().collect(Collectors.toList());
      if (allOut.size() > 0) {
        log.debug("Standard output of process:");
        for (String s : allOut) {
          log.debug("> " + s);
        }
      }
    }
    BufferedReader processErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    List<String> allErr = processErr.lines().collect(Collectors.toList());
    if (allErr.size() > 0) {
      log.info("Error output of process:");
      List<String> errors = new ArrayList<>();
      for (String s : allErr) {
        if (s != null && s.indexOf("Error response from daemon") >= 0 && s.indexOf("Container") >= 0) {
          // Docker error: cannot find a container
          errors.add(s);
        }
        log.info("> " + s);
      }
      if (errors.size() >= 0) {
        throw new ModelExecutorException("Executor error: " + errors.stream().collect(Collectors.joining(". ")));
      }
    }
  }

  /**
   * Executes the script and passes the dataset as an argument.
   *
   * @param dataset to be processed
   * @param script to be executed
   * @throws ModelExecutorException the model executor exception
   */
  protected abstract void execute(ModelFile dataset, ScriptFile script) throws ModelExecutorException;
}
