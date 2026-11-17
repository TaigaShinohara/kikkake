# きっかけ (Kikkake)

**きっかけを置くだけで、スマホが動く。**

自動化アプリをもっと簡単に。①きっかけ → ②やること → ③条件（任意）を、色分けされたブロックから選ぶ3ステップで、誰でも自動化が作れるAndroidアプリです。

- きっかけ7種：時刻／電池が少ない／Wi-Fi／充電開始／ケーブルを抜いたら／ヘッドホン／アプリを開いたら
- やること16種：通知・声で知らせる・音・おやすみモード・ライト・明るさ など
- データは端末の中だけ。外部に送信しません（プライバシーポリシーは docs/policy/index.html）

## 開発

- パッケージ: com.taigashinohara.kikkake ／ 現在のバージョン: 1.4.3
- ビルド: `./gradlew assembleDebug`（Gradle 8.9 / JDK 21 / Android SDK 35-36）
- 仕様: docs/SPEC.md ／ アート方針: docs/ART_DIRECTION.md ／ UIカタログ: docs/UI_CATALOG.md

## ライセンス

MIT License
