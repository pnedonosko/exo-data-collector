package org.exoplatform.prediction;

import java.io.File;

/**
 * The ScriptsExecutor interface
 */
public interface ScriptsExecutor {

  String train(File dataset);

  void predict(File dataset);
}
