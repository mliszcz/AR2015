#!/bin/bash

for series in {1..4}; do
  for i in {1..12}; do
    sbt "run 12 5 21 $i"
  done
done
