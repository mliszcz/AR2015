#!/bin/bash

pushd `which start-multinode-spark-cluster.sh`

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <number-of-nodes>"
    popd
    exit 1
fi

SPARK_IS_RUNNING=`netstat -nlt | grep ':7077'`
if [ "$SPARK_IS_RUNNING" != "" ]; then
  echo "Spark is already running on the node $HOSTNAME. Please try to run on another WN"
  popd
  exit 2
fi

SPARK_MASTER_HOST=`hostname`
SPARK_MASTER_WEBUI_PORT=8080
SPARK_MASTER_PORT=7077
FIRST_WORKER=8081
SPARK_WORKER_PORT=8082
MASTER_ENDPOINT="spark://`hostname`:$SPARK_MASTER_PORT"
if [ "$PBS_NODEFILE" == "" ]; then
  REMOTE_HOSTS=""
else
  REMOTE_HOSTS=`cat $PBS_NODEFILE | uniq | grep -v $SPARK_MASTER_HOST`
  HOSTS_ARRAY=($REMOTE_HOSTS)
  REMOTE_HOSTS=${HOSTS_ARRAY[@]:0:$1}
fi



echo "----------------------- Starting Spark Master on Headnode"
$SPARK_HOME/sbin/spark-daemon.sh start org.apache.spark.deploy.master.Master 1 --ip $SPARK_MASTER_HOST --port $SPARK_MASTER_PORT --webui-port $SPARK_MASTER_WEBUI_PORT
EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
  echo "Can not setup Spark cluster"
  popd
  exit $EXIT_CODE
fi

echo "----------------------- Starting Worker on Headnode"
$SPARK_HOME/sbin/spark-daemon.sh start org.apache.spark.deploy.worker.Worker $FIRST_WORKER $MASTER_ENDPOINT --webui-port $FIRST_WORKER
EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
  echo "Can not setup Spark cluster"
  popd
  exit $EXIT_CODE
fi
# $BIGDATA/check_processes.sh

echo "----------------------- Starting Workers on remote nodes"
for WORKER_HOST in $REMOTE_HOSTS
do
  echo "---------" $WORKER_HOST
  SPARK_WORKER_CORES=`cat $PBS_NODEFILE | grep $WORKER_HOST | wc -l`
  pbsdsh -h $WORKER_HOST $BIGDATA/configureSparkHostWorkers.sh $MASTER_ENDPOINT $SPARK_WORKER_PORT $SPARK_WORKER_CORES $USER $BIGDATA $LOADEDMODULES &
  EXIT_CODE=$?
  if [ $EXIT_CODE -ne 0 ]; then
    echo "Can not setup Spark cluster"
    popd
    exit $EXIT_CODE
  fi
  # pbsdsh -h $WORKER_HOST $BIGDATA/check_processes.sh
  SPARK_WORKER_PORT=$(($SPARK_WORKER_PORT+1))
  sleep 2
done
sleep 5
echo 'Spark cluster has been setup !'
popd
