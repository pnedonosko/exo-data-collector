#!/bin/sh
USERNAME="$(whoami)";
UID="$(id -u $USERNAME)";
eval 'docker run --user=$UID -v $1:/tmp -w /tmp tensorflow/tensorflow python $2 $3'
