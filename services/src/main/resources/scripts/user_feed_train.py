import sys
import shutil
import os
import math
import datetime

import json

from sklearn import metrics
import tensorflow as tf
from tensorflow.python.data import Dataset

from datasetutils import *

tf.logging.set_verbosity(tf.logging.ERROR)
pd.options.display.max_columns = 10
pd.options.display.max_rows = 20
pd.options.display.width = 500

pd.options.display.float_format = '{:.5f}'.format


def deltatime_ms(to_time, from_time):
  return (to_time - from_time).total_seconds() * 1000 #.microseconds / 1000


# Train the model: predict in iterations
def train_model(learning_rate, steps, batch_size, input_feature):
  """Trains a linear regression model of one feature.

  Args:
    learning_rate: A `float`, the learning rate.
    steps: A non-zero `int`, the total number of training steps. A training step
      consists of a forward and backward pass using a single batch.
    batch_size: A non-zero `int`, the batch size.
    input_feature: A `string` specifying a column from `feed_dataframe`
      to use as input feature.
  """

  periods = 10
  steps_per_period = steps / periods

  my_feature = input_feature
  my_feature_data = feed_dataframe[[my_feature]]
  my_label = "rank"
  targets = feed_dataframe[my_label]

  # Create feature columns.
  feature_columns = [tf.feature_column.numeric_column(my_feature)]

  # Create input functions.
  training_input_fn = lambda: train_input_fn(my_feature_data, targets, batch_size=batch_size)
  prediction_input_fn = lambda: train_input_fn(my_feature_data, targets, num_epochs=1, shuffle=False)

  # Create a linear regressor object.
  my_optimizer = tf.train.GradientDescentOptimizer(learning_rate=learning_rate)
  my_optimizer = tf.contrib.estimator.clip_gradients_by_norm(my_optimizer, 5.0)
  linear_regressor = tf.estimator.LinearRegressor(
    feature_columns=feature_columns,
    optimizer=my_optimizer,
    model_dir=my_model_dir
  )

  # Train the model, but do so inside a loop so that we can periodically assess
  # loss metrics.
  print("Training model...")
  print("RMSE (on training data):")
  trainstarttime = datetime.datetime.now()
  for period in range(0, periods):
    iterstarttime = datetime.datetime.now()
    # Train the model, starting from the prior state.
    linear_regressor.train(
      input_fn=training_input_fn,
      steps=steps_per_period
    )
    # Take a break and compute predictions.
    predictions = linear_regressor.predict(input_fn=prediction_input_fn)
    predictions = np.array([item['predictions'][0] for item in predictions])

    # Compute loss.
    root_mean_squared_error = math.sqrt(
      metrics.mean_squared_error(predictions, targets))
    # Occasionally print the current loss.
    print("  period %02d : %0.2f" % (period, root_mean_squared_error))
    iterdonetime = datetime.datetime.now()
    print("Analysed data in {}ms".format(deltatime_ms(iterdonetime, iterstarttime)))

  print("Model training finished.")
  print("Training finished in {}ms".format(deltatime_ms(datetime.datetime.now(), trainstarttime)))

  # Output a table with calibration data.
  calibration_data = pd.DataFrame()
  calibration_data["predictions"] = pd.Series(predictions)
  calibration_data["targets"] = pd.Series(targets)
  print(calibration_data.describe(percentiles=[.10, .25, .5, .75, .90]))

  print("Final RMSE (on training data): %0.2f" % root_mean_squared_error)

  return (linear_regressor, calibration_data, root_mean_squared_error)


starttime = datetime.datetime.now()

# Read script arguments:
# 1. dataset file path (mandatory)
# 2. model folder path (optional), if not provided then we create 'model' folder in the dataset folder
if len(sys.argv) == 2:
  my_dataset_file = sys.argv[1]
  my_model_dir = os.path.dirname(os.path.realpath(my_dataset_file)) + "/model"
elif len(sys.argv) == 3:
  my_dataset_file = sys.argv[1]
  my_model_dir = sys.argv[2]
else :
  # Only for developer mode, in prod it should return error
  my_dataset_file = "./developer_data/user3.csv"
  my_model_dir = "./developer_data/model"
  # Try to remove model dir; if failed show an error using try...except on screen
  # TODO clean model folder only for development
  try:
    shutil.rmtree(my_model_dir)
  except OSError as e:
    print ("Error: %s - %s." % (e.filename, e.strerror))

try :
  # Load data
  feed_dataframe = pd.read_csv(my_dataset_file, sep=",")
  loadtime = datetime.datetime.now()
  print("Loaded data in {}ms".format(deltatime_ms(loadtime, starttime)))

  feed_dataframe = preprocess_features(feed_dataframe)
  print("Training data:")
  print(feed_dataframe.describe(percentiles=[.10, .25, .5, .75, .90]))
  # Shufle the dataset
  feed_dataframe = feed_dataframe.sample(frac=1)
  feed_dataframe.reset_index(drop=True)

  # Train a model
  linear_regressor, calibration_data, rmse = train_model(
      learning_rate=0.0003,
      steps=600,
      batch_size=20,
      input_feature="activity_influence")

  metadata = {
    "status" : "READY",
    "dataset" : my_dataset_file,
    "model_dir" : my_model_dir,
    "training_time": "{:.0f}".format(deltatime_ms(datetime.datetime.now(), starttime)),
    "RMSE" : "{:.5f}".format(rmse)
  }

  # Display all graphs.
  #plt.show()
except (OSError, ValueError) as e:
  #print("Error: %s - %s." % (e.filename, e.strerror))
  metadata = {
    "status": "ERROR",
    "dataset": my_dataset_file,
    "model_dir": my_model_dir,
    "training_time": my_model_dir
  }

# Save the model metadata (JSON file)
try:
  my_metadata_file = os.path.dirname(os.path.realpath(my_dataset_file)) + "/model.json"
  with open(my_metadata_file, "w") as mdfile:
    json.dump(metadata, mdfile)
except OSError as e:
  print ("Error: %s - %s." % (e.filename, e.strerror))

