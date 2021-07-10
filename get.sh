#!/bin/bash

startProblem=60
endProblem=78

for (( i = startProblem; i <= endProblem; i++ )); do
  echo "Receiving problem $i"
  curl -sL \
   -X GET \
    -H "Authorization: Bearer $API_TOKEN" \
    --url "https://poses.live/api/problems/$i" \
    -o "problems/$i.problem"
done
