import sys
import shutil
import os
import math
import datetime

import json

from IPython import display
from matplotlib import cm
from matplotlib import gridspec
from matplotlib import pyplot as plt
import numpy as np
import pandas as pd
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

  # Set up to plot the state of our model's line each period.
  plt.figure(figsize=(15, 6))
  plt.subplot(1, 2, 1)
  plt.title("Learned Line by Period")
  plt.ylabel(my_label)
  plt.xlabel(my_feature)
  # TODO Change n to 1000 in prod mode
  sample = feed_dataframe.sample(n=10)
  plt.scatter(sample[my_feature], sample[my_label])
  colors = [cm.coolwarm(x) for x in np.linspace(-1, 1, periods)]

  # Train the model, but do so inside a loop so that we can periodically assess
  # loss metrics.
  print("Training model...")
  print("RMSE (on training data):")
  root_mean_squared_errors = []
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
    # Add the loss metrics from this period to our list.
    root_mean_squared_errors.append(root_mean_squared_error)
    # Finally, track the weights and biases over time.
    # Apply some math to ensure that the data and line are plotted neatly.
    y_extents = np.array([0, sample[my_label].max()])

    weight = linear_regressor.get_variable_value('linear/linear_model/%s/weights' % input_feature)[0]
    bias = linear_regressor.get_variable_value('linear/linear_model/bias_weights')

    x_extents = (y_extents - bias) / weight
    x_extents = np.maximum(np.minimum(x_extents,
                                      sample[my_feature].max()),
                           sample[my_feature].min())
    y_extents = weight * x_extents + bias
    plt.plot(x_extents, y_extents, color=colors[period])

  print("Model training finished.")
  print("Training finished in {}ms".format(deltatime_ms(datetime.datetime.now(), trainstarttime)))

  # Output a graph of loss metrics over periods.
  plt.subplot(1, 2, 2)
  plt.ylabel('RMSE')
  plt.xlabel('Periods')
  plt.title("Root Mean Squared Error vs. Periods")
  plt.tight_layout()
  plt.plot(root_mean_squared_errors)

  # Output a table with calibration data.
  calibration_data = pd.DataFrame()
  calibration_data["predictions"] = pd.Series(predictions)
  calibration_data["targets"] = pd.Series(targets)
  display.display(calibration_data.describe(percentiles=[.10, .25, .5, .75, .90]))

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
  my_dataset_file = "./developer_data/training.csv"
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

  plt.figure(figsize=(15, 6))

  plt.subplot(1, 2, 1).set_title("owner_influence")
  _ = feed_dataframe["owner_influence"].hist()
  plt.subplot(1, 2, 2).set_title("poster_influence")
  _ = feed_dataframe["poster_influence"].hist()

  plt.figure(figsize=(15, 6))
  plt.subplot(1, 2, 1).set_title("participant1_influence")
  _ = feed_dataframe["participant1_influence"].hist()
  plt.subplot(1, 2, 2).set_title("participant2_influence")
  _ = feed_dataframe["participant2_influence"].hist()

  plt.figure(figsize=(15, 6))
  plt.subplot(1, 2, 1).set_title("poster_focus_engineering")
  _ = feed_dataframe["poster_focus_engineering"].hist()
  plt.subplot(1, 2, 2).set_title("poster_focus_other")
  _ = feed_dataframe["poster_focus_other"].hist()

  # Train a model
  linear_regressor, calibration_data, rmse = train_model(
      learning_rate=0.0003,
      steps=600,
      batch_size=20,
      input_feature="activity_influence")

  rmse = 0

  metadata = {
    "status" : "READY",
    "dataset" : my_dataset_file,
    "model_dir" : my_model_dir,
    "training_time": "{:.0f}".format(deltatime_ms(datetime.datetime.now(), starttime)),
    "RMSE" : "{:.5f}".format(rmse)
  }

  # Display all graphs.
  plt.show()
except (ValueError) as e:
  err = "ValueError: {}.".format(str(e))
  print(err)
  metadata = {
    "status": "ERROR",
    "dataset": my_dataset_file,
    "model_dir": my_model_dir,
    "training_time": my_model_dir,
    "error" : err
  }
except (OSError) as e:
  err = "OSError: {}.".format(str(e))
  print(err)
  metadata = {
    "status": "ERROR",
    "dataset": my_dataset_file,
    "model_dir": my_model_dir,
    "training_time": my_model_dir,
    "error" : err
  }

# Save the model metadata (JSON file)
try:
  my_metadata_file = os.path.dirname(os.path.realpath(my_dataset_file)) + "/model.json"
  with open(my_metadata_file, "w") as mdfile:
    json.dump(metadata, mdfile)
except OSError as e:
  print ("Error: %s - %s." % (e.filename, e.strerror))

