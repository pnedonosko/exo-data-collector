package org.exoplatform.prediction;

import java.io.File;

public interface TrainingExecutor {
  String train(File dataset, String trainingScriptPath);
}
