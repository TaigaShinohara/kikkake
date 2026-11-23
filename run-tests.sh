#!/bin/bash
# きっかけ — 単体テスト＋JVM画面検証ランナー（docs/VERIFICATION.md）。
#
#   ./run-tests.sh          全部実行。スクショはゴールデン（app/src/test/screenshots/）と照合
#   ./run-tests.sh record   全部実行。スクショを再記録（目視確認してからゴールデンとして確定）
#
# Gradle の Test タスクはこのVMでは結果集計で落ちる（既知のNPE）ため、コンパイルは Gradle、
# 実行は AGP が組み立てたクラスパスで JUnitCore を直接叩く。通常のマシンでは
# `gradle :app:testDebugUnitTest` でも同じテストが回る。
set -e
cd "$(dirname "$0")"
MODE="${1:-verify}"
case "$MODE" in
  verify) ROBORAZZI_PROP="-Droborazzi.test.verify=true" ;;
  record) ROBORAZZI_PROP="-Droborazzi.test.record=true" ;;
  *) echo "usage: $0 [verify|record]" >&2; exit 2 ;;
esac

# コンパイル＋テスト設定生成＋実行クラスパスの取り出し（build.sh がビルド直列化ロック経由で回す）
./build.sh :app:compileDebugUnitTestKotlin :app:generateDebugUnitTestConfig :app:printUnitTestClasspath

# Robolectric の android-all は Java の直接通信では取得できないため、
# ローカルミラーのディスクキャッシュからオフライン用ディレクトリへ種まきする
EXTRA_PROPS=()
ROBO_DIR="$PWD/app/build/robolectric-deps"
ANDROID_ALL_JAR=~/workspace/tools/mirror-cache/central/org/robolectric/android-all-instrumented/15-robolectric-12650502-i7/android-all-instrumented-15-robolectric-12650502-i7.jar
if [ -f "$ANDROID_ALL_JAR" ]; then
  mkdir -p "$ROBO_DIR"
  cp -n "$ANDROID_ALL_JAR" "$ROBO_DIR/" 2>/dev/null || true
  EXTRA_PROPS+=(-Drobolectric.offline=true "-Drobolectric.dependency.dir=$ROBO_DIR")
else
  EXTRA_PROPS+=(-Drobolectric.dependency.repo.url=http://127.0.0.1:18080/central -Drobolectric.dependency.repo.id=central)
fi

TEST_CLASSES="com.taigashinohara.kikkake.LogicTest com.taigashinohara.kikkake.ScreenFlowTest com.taigashinohara.kikkake.ScreenshotTest com.taigashinohara.kikkake.LargeFontScreenshotTest"
echo "Running: $TEST_CLASSES"
# 作業ディレクトリは app/：スクショのゴールデンを src/test/screenshots/ の相対パスで指すため
cd app
# Robolectric はネイティブライブラリを java.io.tmpdir へ展開する。このVMの /tmp は 512MB の
# tmpfs で他プロジェクトの展開残骸と奪い合い満杯になるため、ワークスペース側へ逃がす
mkdir -p build/jvmtmp
~/workspace/tools/with-build-lock.sh ~/workspace/tools/jdk21/bin/java \
  -Xmx1536m -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Duser.timezone=Asia/Tokyo \
  "-Djava.io.tmpdir=$PWD/build/jvmtmp" \
  "$ROBORAZZI_PROP" "${EXTRA_PROPS[@]}" \
  -cp "$(cat build/unitTestClasspath.txt)" \
  org.junit.runner.JUnitCore $TEST_CLASSES
