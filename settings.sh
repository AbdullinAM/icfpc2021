PROBLEM_DIR="problems"
PROBLEM_LAST_INDEX=106

POSES_LIVE_COOKIE=2bc36af7-7a6e-45a2-8e13-ae43434a29a5

SOLUTION_DIR="solutions"

function get_problem_current_id () {
    i=$1

    page=`curl -sL \
      -X GET \
      --cookie "session=$POSES_LIVE_COOKIE" \
      --url "https://poses.live/problems/$i"`
    link=`grep -E -o '<a href="/solutions/([a-z0-9-]*)">' <<< "$page" | head -n 1`
    uuid=`sed -E 's/<a href="\/solutions\/([a-z0-9-]*)">/\\1/' <<< "$link"`

    last_seen_uuid=$uuid
}

function try_post_problem_by_file () {
	file=$1

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
}
