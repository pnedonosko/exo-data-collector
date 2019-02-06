import sys
import os
import math
import datetime

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


starttime = datetime.datetime.now()

# Read script arguments:
# 1. dataset file path (mandatory)
# 2. model folder path (mandatory), if not provided then we assume 'model' folder in the dataset folder
if len(sys.argv) == 2:
    my_data_file = sys.argv[1]
    my_model_dir = os.path.dirname(os.path.realpath(my_data_file)) + "/model"
    my_predicted_file = os.path.dirname(os.path.realpath(my_data_file)) + "/predicted.csv"
elif len(sys.argv) == 3:
    my_data_file = sys.argv[1]
    my_model_dir = sys.argv[2]
    my_predicted_file = os.path.dirname(os.path.realpath(my_data_file)) + "/predicted.csv"
else :
    # Only for developer mode, in prod it should return error
    my_data_file = "./developer_data/predict.csv"
    my_model_dir = "./developer_data/model"
    my_predicted_file = "./developer_data/predicted.csv"

# Load data
feed_dataframe = pd.read_csv(my_data_file, sep=",")
loadtime = datetime.datetime.now()
print("Loaded data in {}ms".format(deltatime_ms(loadtime, starttime)))

feed_dataset = preprocess_features(feed_dataframe, withRank=False)
print("Training data:")
print(feed_dataset.describe(percentiles=[.10, .25, .5, .75, .90]))

##########################
# Load the existing model
def load_model(input_feature):
  """Loads a linear regression model.

  Args:
    learning_rate: A `float`, the learning rate.
    steps: A non-zero `int`, the total number of training steps. A training step
      consists of a forward and backward pass using a single batch.
    batch_size: A non-zero `int`, the batch size.
    input_feature: A `string` specifying a column from `feed_dataframe`
      to use as input feature.
  """
  loadstarttime = datetime.datetime.now()

  my_feature = input_feature

  # Create feature columns.
  feature_columns = [tf.feature_column.numeric_column(my_feature)]

  # Create a linear regressor object.
  linear_regressor = tf.estimator.LinearRegressor(
    feature_columns=feature_columns,
    model_dir = my_model_dir
  )

  print("Model loading finished.")
  print("Loading finished in {}ms".format(deltatime_ms(datetime.datetime.now(), loadstarttime)))

  return linear_regressor

# Infer a prediction:
feed_dataset["rank"] = 1
my_targets = feed_dataset["rank"]
linear_regressor = load_model(input_feature="activity_influence")

prediction_input_fn = lambda: predict_input_fn(feed_dataset, my_targets)

predictstarttime = datetime.datetime.now()
my_predictions = linear_regressor.predict(input_fn=prediction_input_fn)
predictdonetime = datetime.datetime.now()
print("Predicted in {}ms".format(deltatime_ms(predictdonetime, predictstarttime)))
my_predictions = np.array([item['predictions'][0] for item in my_predictions])

root_mean_squared_error = math.sqrt(
    metrics.mean_squared_error(my_predictions, my_targets))
print("Final RMSE (on my data): %0.2f" % root_mean_squared_error)

predicted_dataframe = pd.DataFrame({'Predictions': my_predictions,'Targets': my_targets})
print(predicted_dataframe.describe(percentiles=[.10, .25, .5, .75, .90]))

# Save the predictions into the original feed dataset file
#feed_dataframe["rank_predicted"] = my_predictions
predicted_dataframe = pd.DataFrame()
predicted_dataframe["id"] = feed_dataframe["id"]
predicted_dataframe["rank"] = my_predictions

# TODO In some cases we may need to cut things to 1.0
#feed_dataframe["rank_predicted"] = (
#  feed_dataframe["rank_predicted"].apply(lambda x: min(x, 1.0)))
#feed_dataframe.to_csv(my_data_file, sep=",", index=False)
predicted_dataframe = predicted_dataframe.sort_values('rank')
predicted_dataframe= predicted_dataframe.drop(columns="rank")
predicted_dataframe.to_csv(my_predicted_file, sep=",", index=False)

