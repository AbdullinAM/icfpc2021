#!/bin/bash

source api_key.sh
source settings.sh

for file in solutions/*.sol; do
    try_post_problem_by_file $file
done
