#!/usr/bin/env bash

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <results-file>"
    exit 1
fi

gnuplot -e "INPUT_FILE='$1'" standard_time.gnuplot
