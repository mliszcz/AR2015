#!/usr/bin/env bash
#PBS -l nodes=4:ppn=12

DIR="$HOME/AR2015/lab02"

source $PLG_GROUPS_STORAGE/plgg-spark/set_env_spark-1.0.0.sh

ENVIRON="cluster"
# ENVIRON="local"

LOCAL_NODES="4" # for local env only

NODES_MIN="10"
NODES_MAX="10"

GRAPH="generate"
# GRAPH="$DIR/src/main/resources/web-Stanford.txt"

GRAPH_SIZE="1000" # for generated graphs only

SERIES="4"

for NODES in `seq $NODES_MIN $NODES_MAX`; do

    echo "NODES: $NODES"

    $DIR/scripts/start-cluster.sh $NODES

    $SPARK_HOME/bin/spark-submit --master spark://$HOSTNAME:7077 \
        --class Main $DIR/target/scala-2.10/ar-lab02_2.10-0.1.0.jar \
         $ENVIRON $LOCAL_NODES $SERIES $GRAPH $GRAPH_SIZE

    $DIR/scripts/stop-cluster.sh $NODES

done
