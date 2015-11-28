#!/bin/bash

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <1M|10M|100M> <samples>"
  exit 1
fi

now=`date "+%Y%m%d_%H%M%S"`
tmpdir="tmp-cli-$now"
dir="./data"
bucket="s3://$tmpdir"

file1M="$dir/1M.dat"
file10M="$dir/10M.dat"
file100M="$dir/100M.dat"

ONE="1"
samples1M=${2:-$ONE}
samples10M=${2:-$ONE}
samples100M=${2:-$ONE}

s3cmd rb --force $bucket
s3cmd mb $bucket

function RUN_UP_DOWN_TEST () {

  START=$(date +%s.%N)
  for i in `seq 1 $SAMPLES`; do
    s3cmd put $FILE "$bucket/${NUM}M_up_$i.dat" > /dev/null 2>&1
  done
  END=$(date +%s.%N)

  DIFF=$(echo "$END - $START" | bc)
  RESULT=$(echo "($NUM * $SAMPLES) / $DIFF" | bc -l)

  echo "${NUM}M | UP   | $DIFF s | $RESULT MBps"

  START=$(date +%s.%N)
  for i in `seq 1 $SAMPLES`; do
    s3cmd get "$bucket/${NUM}M_up_$i.dat" `mktemp -u` > /dev/null 2>&1
  done
  END=$(date +%s.%N)

  DIFF=$(echo "$END - $START" | bc)
  RESULT=$(echo "($NUM * $SAMPLES) / $DIFF" | bc -l)

  echo "${NUM}M | DOWN | $DIFF s | $RESULT MBps"
}

if [ "$1" == "1M" ]; then

  # ===========================================================================
  # 1M

  NUM="1"
  FILE="$file1M"
  SAMPLES="$samples1M"

  RUN_UP_DOWN_TEST

elif [ "$1" == "10M" ]; then

  # ===========================================================================
  # 10M

  NUM="10"
  FILE="$file10M"
  SAMPLES="$samples10M"

  RUN_UP_DOWN_TEST

elif [ "$1" == "100M" ]; then

  # ===========================================================================
  # 100M

  NUM="100"
  FILE="$file100M"
  SAMPLES="$samples100M"

  RUN_UP_DOWN_TEST

else

  # ===========================================================================
  # error

  echo "Usage: $0 <1M|10M|100M> <samples>"
  exit 1

fi

# =============================================================================

s3cmd rb --force $bucket
