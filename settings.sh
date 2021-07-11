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
