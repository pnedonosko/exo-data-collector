#!/bin/sh
USERNAME="$(whoami)";
UID="$(id -u $USERNAME)";
eval 'docker exec --user=$UID -it $1 python $2 $3'

