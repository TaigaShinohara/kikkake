# バグ修正記録（v1.5.1、2026-11-19）

## 症状（本人報告）
「充電したらライトをつける」自動化が、テスト実行では動くのに、実際に充電しても動かない。

## 原因
AndroidManifest.xml の intent-filter のアクション名の書き間違い。

- 誤：`android.intent.action.ACTION_POWER_CONNECTED`
- 正：`android.intent.action.POWER_CONNECTED`
- 同じく DISCONNECTED 側も誤っていた。

レシーバー側のコード（Intent.ACTION_POWER_CONNECTED との照合、Engine.onChargingStarted）は正しかったが、マニフェストの登録名が一致しないため、充電イベントが一度も届いていなかった。テスト実行はブロードキャストを経由しないため正常に見えた。

影響範囲：充電開始（CHARGING_START）とケーブルを抜いたら（CHARGING_DONE）の2きっかけが、実イベントで発火しない状態だった。ヘッドホン（HEADSET_PLUG の値は正しい）とブートは正常。

## 修正
1. マニフェストの2アクション名を正しい値に修正。
2. ついでにライト（FLASHLIGHT）の状態ずれ対策：従来はアプリ内の保存フラグで点灯状態を判断していたため、クイック設定など手動でライトを消すと次回の切り替えが逆転して見えた。実行時に CameraManager の TorchModeCallback で実際の状態を読んでから切り替えるよう変更。

## 教訓
- マニフェストのアクション名は定数名ではなく「値」で書く。`Intent.ACTION_*` の値が `android.intent.action.*` と一致するとは限らない（POWER_CONNECTED 等）。
- 「テストでは動くが実イベントで動かない」場合は、配信経路（マニフェスト登録・ブロードキャスト名）を疑う。

## 検証
ビルド成功・ユニットテスト11件全合格・APK の versionName 1.5.1（versionCode 19）確認済み。さらに完成した APK 内のマニフェストを直接読み、正しいアクション名（POWER_CONNECTED／POWER_DISCONNECTED）が入っていることまで確認した。実際の充電での発火は実機のみで確認可能（実機待ち行列）。
