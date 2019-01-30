package org.exoplatform.prediction;

import java.io.File;

public interface ScriptsExecutor {
  String train(File dataset);

  void predict(File dataset);
}
