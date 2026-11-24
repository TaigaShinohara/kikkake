# 実機なし検証（交付前チェック）

2026-11-23 の標準化（AGENTS.md「Pre-delivery UI verification standard」）に基づく、
きっかけの JVM 検証の正本。エミュレータの無いビルド VM でも、画面の崩壊・真っ白・
遷移破綻を交付前に捕まえるための仕組み。

## 回し方

```bash
./run-tests.sh          # 全部実行（スクショはゴールデンと照合＝verify）
./run-tests.sh record   # 全部実行（スクショを再記録。目視確認してからゴールデンとして確定）
```

- コンパイルは Gradle（build.sh 経由＝ビルド直列化ロック適用）、実行は JUnitCore を直接叩く。
  Gradle の Test タスクはこの VM では結果集計で落ちる（Executor のループバック通信が遮断される／
  集計 NPE）ため使わない。通常のマシンでは `gradle :app:testDebugUnitTest` でも同じテストが回る。
- 実行クラスパスは `:app:printUnitTestClasspath` が `app/build/unitTestClasspath.txt` へ書き出す。
  注意：Test タスクのクラスパスを inputs.files や taskDependencies 経由で配線すると、
  Gradle が testDebugUnitTest 自体をグラフへ引き込み Executor が走って落ちる。
  コンパイルタスクへだけ依存し doLast で遅延読みするのが正解（QRいっき読みと同方式）。
- 注意2：本体クラスはクラスパス上の `runtime_app_classes_jar` の classes.jar 経由で読まれる。
  その生成タスク（bundleDebugClassesToRuntimeJar）は Test タスクの依存にしかおらず、
  printUnitTestClasspath の依存に入れておかないと、jar が古いまま（追加した画面が
  NoSuchMethodError）／clean 後は無いまま（全件 NoClassDefFoundError）になる（2026-11-24 v2.3.0 で踏んだ）。

## テスト構成（全28件）

| クラス | 件数 | 内容 |
|---|---|---|
| LogicTest | 17 | 純ロジック（従来分） |
| ScreenFlowTest | 4 | Robolectric＋Compose テストルールで実物の KikkakeApp を描き、初回起動→オンボーディング表示／ホームのタイルと操作ラベル／ウィザード Step1→2→3 の遷移と条件チップ表示／ホーム→おすすめフィード→「これを使う」でマクロ追加→ホームにタイル表示、を検証 |
| ScreenshotTest | 5 | Roborazzi ゴールデン照合：ホーム（見本マクロ2件）／オンボーディング入口／ウィザード Step1／ウィザード Step3（電池条件チップ）／おすすめフィード先頭カード |
| LargeFontScreenshotTest | 2 | 同上を fontScale 1.3 で：ホーム／ウィザード Step3 |

ゴールデン画像は `app/src/test/screenshots/`（7枚）。記録したら必ず全枚目視確認する
（崩壊・見切れ・真っ白が無いこと）。最低カバー範囲＝起動直後の画面／オンボーディング入口／
その版で作り替えた画面、を満たす。

## なぜ Paparazzi ではなく Roborazzi か

当初 Paparazzi 1.3.5 で進めたが、Robolectric と同居させると Paparazzi の Renderer が
`android.os.Build` の内部クラスを layoutlib の `_Original_Build` と突き合わせる処理
（configureBuildProperties）で落ちる（内部クラスの顔ぶれが android-all と layoutlib で違う）。
Roborazzi は Robolectric のネイティブグラフィクス上でそのまま撮れるため同居できる。
方式は QRいっき読みの実績方式（captureRoboImage 直接呼び＋ゴールデン照合）に合わせた。

## この VM 固有の注意

- **ComponentActivity の宣言**：Compose テストルールが使う `androidx.activity.ComponentActivity` は
  ライブラリのマニフェストに無く、Robolectric が起動できずに落ちる。`app/src/debug/AndroidManifest.xml`
  で debug ビルドだけに宣言してある（release には含まれない）。
- **android-all のオフライン種まき**：Robolectric の android-all は Java の直接通信では取得できない。
  run-tests.sh がローカルミラーのディスクキャッシュから `app/build/robolectric-deps/` へコピーし、
  `-Drobolectric.offline=true` で読ませる。キャッシュに無い環境では HTTP ミラー経由にフォールバック。
- **java.io.tmpdir の逃がし先**：Robolectric はネイティブライブラリを tmpdir へ展開する。
  この VM の /tmp は 512MB の tmpfs で、他プロジェクトの展開残骸と奪い合って満杯になる
  （"No space left on device" で native runtime のロードが落ちる）。run-tests.sh は
  `-Djava.io.tmpdir=app/build/jvmtmp` を指定してワークスペース側へ逃がしている。
- **ビルド直列化**：コンパイルもテスト実行も with-build-lock.sh 経由（全プロジェクト同時2本まで）。

## 結果（2026-11-23 初回）

- record：全26件合格。ゴールデン6枚を全枚目視確認（ホーム／オンボーディング／Step1／Step3／
  大きな文字2枚）— 崩壊・見切れ・真っ白なし。大きな文字でもタイルの「テスト」が欠けず、
  Step3 の条件チップは FlowRow で行折り返しになることを確認（v2.2.4／v2.2.5 の事故の再発なし）。
- verify：全26件合格（ゴールデン照合一致）。

## 結果（2026-11-24、v2.3.0）

- verify：全28件合格（ゴールデン照合一致）。ゴールデン7枚を目視確認
  （フィード先頭カード＝フローチップ・ひとこと・追加済み表示に崩れなし／ホーム2枚は差替後も崩れなし）。
- フィード遷移テストの教訓：LazyColumn は画面外の項目を合成しない／合成しても bounds 0 で押せない。
  テストでは対象を `performScrollToIndex` で画面内へ入れてから探す・押すこと。
  部分一致のテキスト検索はスナックバー文言にも当たるため、タイル名は完全一致で探すこと。

## 結果（2026-11-24、v2.4.0）

- verify：全28件合格（ゴールデン照合一致）。フィードのゴールデンをワクワク仕様
  （色巡回＋グラデ帯＋バッジ）で再記録し目視確認（先頭カードのタイトル折り返し崩れを
  バッジ位置の移動で修正済み）。テンプレは18件に増量したが、フィード遷移テストの
  対象（先頭側の並び）は不変のため既存テストのまま合格。
- release ビルド注意：`lintVitalAnalyzeRelease` が "For input string: 37.2" で落ちるのは
  SDK プラットフォーム android-37.2 の版文字列を旧 AGP lint が解析できない環境要因
  （アプリの不具合ではない）。当面は `-x lintVitalAnalyzeRelease` を付けてビルドする。

## 結果（2026-11-24、v2.5.0）

- verify：全28件合格（ゴールデン照合一致）。テンプレ31件化に伴いフィードの
  件数表示が「全31件」になった分だけゴールデンを再記録し目視確認（先頭カード群は不変）。
  新規テンプレ13件は全てエンジン側の param 仕様（SUN_EVENT の "sunset@緯度,経度"、
  LOCATION の "arrive@…"、REPEAT の "3回"、WALLPAPER の色名、NOTIFICATION_POSTED の
  "パッケージ|キーワード"）を実コードで確認してから作成した。

## 結果（2026-11-24、v2.6.0）

- verify：全29件合格（ゴールデン照合一致）。フィードのホーム化に伴いゴールデンを
  組み替え：起動直後＝ launch_feed.png（フロートボタン「マイセット（2件）」が見える）、
  マイセット画面＝ myset.png（遷移テスト内で撮影する方式を新設）、旧 home_with_macros 系は廃止。
  遷移テストは「起動→フィード」「FAB→マイセット」「マイセット→ウィザード」を実遷移で確認。
  両画面とも目視確認済み（崩壊・見切れなし）。

## v2.7.0（2026-11-24）UX審査の致命的4件の再設計

- verify：全30件合格（ゴールデン照合一致）。新規＝初回フィード＋試し置きカードの
  フローテスト2件（表示／電池選択→追加→「今すぐ試す」表示）とゴールデン
  launch_feed_trial.png。旧オンボーディングのテスト・ゴールデンは廃止。
- ゴールデン変化：launch_feed.png（導入文から「全31件」削除、追加済みカードの
  チップ整理）、myset.png（ヘッダーは「使い方」のみ、管理系は「その他の設定」に
  畳み）。全画面を目視確認済み（崩壊・見切れなし、試し置きカードの3択と
  「許可が要る」チップも表示確認）。
