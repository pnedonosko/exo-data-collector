#!/bin/sh
MYUSERNAME="$(whoami)";
MYUID="$(id -u $MYUSERNAME)";
eval 'docker exec --user=$MYUID -i $1 python $2 $3'

