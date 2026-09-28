#!/usr/bin/env bash
# 에뮬레이터에서 스토어용 화면을 찍어 docs/screenshots/ 로 가져온다.
#
# 이 내용을 워크플로 안에 바로 적으면 안 된다. android-emulator-runner 는
# script 를 한 줄로 이어 붙여 sh -c 에 넘기기 때문에, 줄바꿈이 사라지면서
# 이어쓰기 역슬래시가 그대로 인자가 되어 버린다(Task '\' not found).
set -euo pipefail

# 이 스크립트가 말하는 것을 전부 build.log 에도 남긴다. 실패했을 때 워크플로가
# 이 파일에서 필요한 줄만 추려 로그 끝에 다시 찍어 준다 — 에뮬레이터 잡의
# 원본 로그는 뒤따르는 캐시 정리 소음에 묻혀 꼬리에 원인이 남지 않는다.
exec > >(tee build.log) 2>&1

# 앱 폴더가 아니다. connectedAndroidTest 는 끝나고 앱을 지우는데, 앱을 지우면
# /sdcard/Android/data/<패키지> 도 같이 사라진다. 찍어 놓고 가져가기 직전에
# 없어진다. 그래서 테스트가 셸로 /sdcard/screenshots 에 찍는다.
REMOTE="/sdcard/screenshots"

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.quoteday.app.ScreenshotTest \
  --console=plain

echo "=== 기기에 찍힌 것 ==="
# 폴더가 없으면 위 단계부터 보여 준다. 테스트가 아예 안 돌았는지, 돌았는데
# 다른 곳에 썼는지 가려야 한다.
adb shell ls -l "$REMOTE" || {
  echo "(기기에 $REMOTE 가 없다)"
  adb shell ls -l /sdcard
}

# 폴더를 통째로 가져온다. 안에 docs/screenshots 가 생긴다.
#
# `adb pull "$REMOTE/." docs/screenshots/` 는 쓰지 않는다. 끝의 `/.` 는 cp 의
# 관용구지 adb 의 것이 아니다. adb 는 그 경로를 그대로 stat 해서 실패한다.
mkdir -p docs
rm -rf docs/screenshots
adb pull "$REMOTE" docs/

echo "=== 가져온 것 ==="
ls -la docs/screenshots
