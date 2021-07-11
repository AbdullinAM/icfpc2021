#!/bin/bash

source api_key.sh
source settings.sh

for file in solutions/*.sol; do
  filename="$(basename -- $file)"
  extension="${filename##*.}"
  problem_id="${filename%.*}"

  echo "Getting current stats for problem $problem_id"
  get_problem_current_id $problem_id

  json=`curl -sL \
    -X GET \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $API_TOKEN" \
    --data @"./$file" \
    --url "https://poses.live/api/problems/$problem_id/solutions/$last_seen_uuid"`

  current_dislikes=`echo $json | jq ".dislikes"`

  solution_dislikes=`./gradlew -q -PmainClass="ru.spbstu.icpfc2021.SolutionsGraderKt" run --args $problem_id`
  
  if [ $current_dislikes -gt $solution_dislikes ]; then
    echo "We are better for $problem_id: $solution_dislikes < $current_dislikes";
    echo "Posting solution for problem $problem_id"
    curl -sL \
      -X POST \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $API_TOKEN" \
      --data @"./$file" \
      --url "https://poses.live/api/problems/$problem_id/solutions"
  fi
done
