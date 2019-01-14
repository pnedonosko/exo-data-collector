# -*- coding: utf-8 -*-
# Read script arguments:
# 1. dataset file path (mandatory)
# 2. model folder path (optional), if not provided then we create 'model' folder in the dataset folder
import sys
import os
if len(sys.argv) == 2:
  my_dataset_file = sys.argv[1]
  my_model_dir = os.path.dirname(os.path.realpath(my_dataset_file)) + '/model'
  if not os.path.exists(my_model_dir):
    os.makedirs(my_model_dir)
  file_dataset = open(my_dataset_file, 'r')
  file_model = open(my_model_dir + "/model.json", 'w')
  file_model.write(file_dataset.read())
  file_model.close()
  file_dataset.close()
  print "One arg"
elif len(sys.argv) == 3:
  my_dataset_file = sys.argv[1]
  my_model_dir = sys.argv[2]
  file_dataset = open(my_dataset_file,'r')
  file_model = open(my_model_dir + '/model.json', 'w')
  file_model.write(file_dataset.read())
  file_model.close()
  file_dataset.close()
  print "Two args"
else :
  # Only for developer mode, in prod it should return error
  my_dataset_file = "./developer_data/user3.csv"
  my_model_dir = "./developer_data/model"
  print "Else"
