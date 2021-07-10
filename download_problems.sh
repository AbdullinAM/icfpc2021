#!/bin/bash

PROBLEM_DIR="problems"
PROBLEM_LAST_INDEX=88

for i in $(seq 1 $PROBLEM_LAST_INDEX); do
    wget https://poses.live/problems/$i/download -O $PROBLEM_DIR/$i.problem
done
