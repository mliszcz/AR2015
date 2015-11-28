#!/bin/bash

dir="./data"

rm -rf $dir
mkdir -p $dir

dd if=/dev/urandom of=$dir/1M.dat bs=1048576 count=1
dd if=/dev/urandom of=$dir/10M.dat bs=1048567 count=10
dd if=/dev/urandom of=$dir/100M.dat bs=1048567 count=100
