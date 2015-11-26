#!/usr/bin/env bash

# DIR="$HOME/Documents/studia/AR2015/lab02"
DIR="/var/fpwork/liszcz/AR2015/lab02"

# SPARK_HOME="$HOME/Documents/spark-1.1.0-bin-hadoop2.4"
# SPARK_HOME="$HOME/Documents/spark-1.5.2-bin-hadoop2.6"
SPARK_HOME="/var/fpwork/liszcz/spark-1.5.2-bin-hadoop2.6"

ENVIRON="cluster"
# ENVIRON="local"

LOCAL_NODES="4" # for local env only

NODES_MIN="1"
NODES_MAX="12"

# GRAPH="generate"
# GRAPH="$DIR/src/main/resources/ca-GrQc.txt"
# GRAPH="$DIR/src/main/resources/web-Stanford.txt"
# GRAPH="$DIR/src/main/resources/p2p-Gnutella24.txt"
GRAPH="$DIR/src/main/resources/p2p-Gnutella31.txt"

GRAPH_SIZE="50000" # for generated graphs only

SERIES="4"

for NODES in `seq $NODES_MIN $NODES_MAX`; do

    RES=`$SPARK_HOME/bin/spark-submit \
        --master "local[$NODES]" \
        --driver-memory 4g \
        --class lab02.Main \
        $DIR/target/scala-2.10/ar-lab02_2.10-0.1.0.jar \
        $ENVIRON $LOCAL_NODES $SERIES $GRAPH $GRAPH_SIZE 2>/dev/null`

    echo "$NODES $RES"

done
