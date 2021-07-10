#!/bin/bash

startProblem=1
endProblem=88

for (( i = startProblem; i <= endProblem; i++ )); do
  echo "Receiving problem $i"
  curl -sL \
   -X GET \
    -H "Authorization: Bearer $API_TOKEN" \
    --url "https://poses.live/api/problems/$i" \
    -o "problems/$i.problem"
done
