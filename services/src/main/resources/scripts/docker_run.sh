#!/bin/sh
MYUSERNAME="$(whoami)";
MYUID="$(id -u $MYUSERNAME)";
eval 'docker run --user=$MYUID -v $1:/tmp -w /tmp tensorflow/tensorflow:1.12.0-py3 python $2 $3'
