#!/bin/bash

for file in solutions/*.sol; do
  filename="$(basename -- $file)"
  extension="${filename##*.}"
  filename="${filename%.*}"
  echo "Posting solution for problem $filename"
  curl -sL \
   -X POST \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $API_TOKEN" \
    --data @"./$file" \
    --url "https://poses.live/api/problems/$filename/solutions"
done