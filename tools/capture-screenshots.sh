#!/usr/bin/env bash
# 에뮬레이터에서 스토어용 화면을 찍어 docs/screenshots/ 로 가져온다.
#
# 이 내용을 워크플로 안에 바로 적으면 안 된다. android-emulator-runner 는
# script 를 한 줄로 이어 붙여 sh -c 에 넘기기 때문에, 줄바꿈이 사라지면서
# 이어쓰기 역슬래시가 그대로 인자가 되어 버린다(Task '\' not found).
set -euo pipefail

PKG=com.quoteday.app
REMOTE="/sdcard/Android/data/$PKG/files/screenshots"

# 로그를 파일로도 남긴다. 실패했을 때 워크플로가 이 파일에서 원인만 추려
# 다시 찍어 준다 — 에뮬레이터 잡의 원본 로그는 캐시 정리 소음에 묻힌다.
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.quoteday.app.ScreenshotTest \
  --console=plain 2>&1 | tee build.log

mkdir -p docs/screenshots

# 기기에 실제로 뭐가 생겼는지 먼저 남긴다. 못 가져왔을 때 원인이 경로인지
# 테스트인지 로그만 보고 가릴 수 있어야 한다.
adb shell ls -l "$REMOTE" || true

adb pull "$REMOTE/." docs/screenshots/

ls -la docs/screenshots
