#!/bin/bash

pushd `which start-multinode-spark-cluster.sh`

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <number-of-nodes>"
    popd
    exit 1
fi

SPARK_MASTER_HOST=`hostname`
SPARK_MASTER_WEBUI_PORT=8080
SPARK_MASTER_PORT=7077
MASTER_ENDPOINT="spark://`hostname`:$SPARK_MASTER_PORT"
if [ "$PBS_NODEFILE" == "" ]; then
  REMOTE_HOSTS=""
else
  REMOTE_HOSTS=`cat $PBS_NODEFILE | uniq | grep -v $SPARK_MASTER_HOST`
  HOSTS_ARRAY=($REMOTE_HOSTS)
  REMOTE_HOSTS=${HOSTS_ARRAY[@]:0:$1}
fi
FIRST_WORKER=8081
SPARK_WORKER_PORT=8082
CPU_PER_WORKER=12

echo "----------------------- Stopping Workers on remote nodes"
for WORKER_HOST in $REMOTE_HOSTS
do
  echo "---------" $WORKER_HOST
  echo `pbsdsh -h $WORKER_HOST $BIGDATA/killSparkWorker.sh`
  sleep 4
done


echo "----------------------- Stopping Worker on Headnode"
$SPARK_HOME/sbin/spark-daemon.sh stop org.apache.spark.deploy.worker.Worker 8081

echo "----------------------- Stopping Master on Headnode"
$SPARK_HOME/sbin/stop-master.sh

popd
