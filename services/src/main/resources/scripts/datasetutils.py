import math
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow.python.data import Dataset

def preprocess_features(feed_dataframe):
  """Prepares input features.

  Args:
    feed_dataframe: A Pandas DataFrame expected to contain data of user data set.
  Returns:
    A DataFrame that contains the features to be used for the model, including
    synthetic features.
  """
  selected_features = feed_dataframe[
    [
      ## "type_content",
     #"type_social",
     #"type_calendar",
     #"type_forum",
     #"type_wiki",
     #"type_poll",
     #"type_other",
     #"owner_type_organization",
     #"owner_type_space",
     "owner_influence",
     #"number_of_likes",
     #"number_of_comments",
     "reactivity", # TODO don't use it - it's part of the target
     #"is_mentions_me",
     #"is_mentions_connections",
     #"is_commented_by_me",
     #"is_commented_by_connetions",
     #"is_liked_by_me",
     #"is_liked_by_connections",
     #"poster_gender_male",
     #"poster_gender_female",
     #"poster_is_employee",
     #"poster_is_lead",
     #"poster_is_in_connections",
     #"poster_focus_engineering",
     #"poster_focus_sales",
     #"poster_focus_marketing",
     #"poster_focus_management",
     #"poster_focus_financial",
     #"poster_focus_other",
     "poster_influence",
     #"participant1_conversed",
     #"participant1_favored",
     #"participant1_gender_male",
     #"participant1_gender_female",
     #"participant1_is_employee",
     #"participant1_is_lead",
     #"participant1_is_in_connections",
     #"participant1_focus_engineering",
     #"participant1_focus_sales",
     #"participant1_focus_marketing",
     #"participant1_focus_management",
     #"participant1_focus_financial",
     #"participant1_focus_other",
     "participant1_influence",
     #"participant2_conversed",
     #"participant2_favored",
     #"participant2_gender_male",
     #"participant2_gender_female",
     #"participant2_is_employee",
     #"participant2_is_lead",
     #"participant2_is_in_connections",
     #"participant2_focus_engineering",
     #"participant2_focus_sales",
     #"participant2_focus_marketing",
     #"participant2_focus_management",
     #"participant2_focus_financial",
     #"participant2_focus_other",
     "participant2_influence",
     #"participant3_conversed",
     #"participant3_favored",
     #"participant3_gender_male",
     #"participant3_gender_female",
     #"participant3_is_employee",
     #"participant3_is_lead",
     #"participant3_is_in_connections",
     #"participant3_focus_engineering",
     #"participant3_focus_sales",
     #"participant3_focus_marketing",
     #"participant3_focus_management",
     #"participant3_focus_financial",
     #"participant3_focus_other",
     "participant3_influence",
     #"participant4_conversed",
     #"participant4_favored",
     #"participant4_gender_male",
     #"participant4_gender_female",
     #"participant4_is_employee",
     #"participant4_is_lead",
     #"participant4_is_in_connections",
     #"participant4_focus_engineering",
     #"participant4_focus_sales",
     #"participant4_focus_marketing",
     #"participant4_focus_management",
     #"participant4_focus_financial",
     #"participant4_focus_other",
     "participant4_influence",
     #"participant5_conversed",
     #"participant5_favored",
     #"participant5_gender_male",
     #"participant5_gender_female",
     #"participant5_is_employee",
     #"participant5_is_lead",
     #"participant5_is_in_connections",
     #"participant5_focus_engineering",
     #"participant5_focus_sales",
     #"participant5_focus_marketing",
     #"participant5_focus_management",
     #"participant5_focus_financial",
     #"participant5_focus_other",
     "participant5_influence",
     "rank"
     ]]

  processed_features = selected_features.copy()

  # Arithmetic regression with single feature:
  # Make synthetic feature as a arithmetic mean of all influencers:
  processed_features["activity_influence"] = (
      (processed_features["owner_influence"] + processed_features["poster_influence"]
       + processed_features["participant1_influence"] + processed_features["participant2_influence"]
       + processed_features["participant3_influence"] + processed_features["participant4_influence"]
       + processed_features["participant5_influence"]
       + processed_features["reactivity"] * 0.9) / 8) #
  # Clip outliers: lesser of 0.2 become equal 0.2
  processed_features["activity_influence"] = (
    processed_features["activity_influence"]).apply(lambda x: max(x, 0.2))

  return processed_features


# input function for training
def train_input_fn(features, targets, batch_size=1, shuffle=True, num_epochs=None):
  """Trains a linear regression model of one feature.

  Args:
    features: pandas DataFrame of features
    targets: pandas DataFrame of targets
    batch_size: Size of batches to be passed to the model
    shuffle: True or False. Whether to shuffle the data.
    num_epochs: Number of epochs for which data should be repeated. None = repeat indefinitely
  Returns:
    Tuple of (features, labels) for next data batch
  """

  # Convert pandas data into a dict of np arrays.
  features = {key: np.array(value) for key, value in dict(features).items()}

  # Construct a dataset, and configure batching/repeating.
  ds = Dataset.from_tensor_slices((features, targets))  # warning: 2GB limit
  ds = ds.batch(batch_size).repeat(num_epochs)

  # Shuffle the data, if specified.
  if shuffle:
    ds = ds.shuffle(buffer_size=10000)

  # Return the next batch of data.
  features, labels = ds.make_one_shot_iterator().get_next()
  return features, labels

# input function for prediction
def predict_input_fn(features, targets, batch_size=1):
  """Trains a linear regression model of one feature.

  Args:
    features: pandas DataFrame of features
    targets: pandas DataFrame of targets (assumed all 1 for inference of predictions)
    batch_size: Size of batches to be passed to the model
    num_epochs: Number of epochs for which data should be repeated. None = repeat indefinitely
  Returns:
    Tuple of (features, labels) for next data batch
  """

  # Convert pandas data into a dict of np arrays.
  features = {key: np.array(value) for key, value in dict(features).items()}

  # Construct a dataset, and configure batching/repeating.
  ds = Dataset.from_tensor_slices((features, targets))  # warning: 2GB limit
  ds = ds.batch(batch_size)

  # Return the next batch of data.
  features, labels = ds.make_one_shot_iterator().get_next()
  return features, labels
