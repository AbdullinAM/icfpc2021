#!/bin/bash

source settings.sh

for i in $(seq 1 $PROBLEM_LAST_INDEX); do
  	echo "Getting solution id for problem $i"
    get_problem_current_id $i
    echo "$last_seen_uuid" > "$SOLUTION_DIR/$i.uuid"
done
