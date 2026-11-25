plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.taigashinohara.kikkake"
    compileSdk = 36
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "com.taigashinohara.kikkake"
        minSdk = 26
        targetSdk = 36
        versionCode = 44
        versionName = "2.11.0"
    }
    signingConfigs {
        val ksFile = rootProject.file(".local/kikkake-debug.jks")
        // Secrets live in .env (gitignored): KIKKAKE_KEYSTORE_PASSWORD=<password>
        val envPass = rootProject.file(".env").takeIf { it.exists() }?.readLines()
            ?.firstOrNull { it.startsWith("KIKKAKE_KEYSTORE_PASSWORD=") }
            ?.substringAfter("=")?.trim()?.takeIf { it.isNotBlank() }
        val ksPass = envPass ?: rootProject.file(".local/keystore-password.txt").takeIf { it.exists() }?.readText()?.trim()
        if (ksFile.exists() && !ksPass.isNullOrBlank()) {
            create("stable") {
                storeFile = ksFile
                storePassword = ksPass
                keyAlias = "kikkake"
                keyPassword = ksPass
            }
        }
    }
    buildTypes {
        debug { signingConfigs.findByName("stable")?.let { signingConfig = it } }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("stable")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    // 実機なし検証（標準 2026-11-23）：Robolectric がアプリのリソースを読めるようにする
    testOptions { unitTests.isIncludeAndroidResources = true }
}

// 実機なし検証の実行設定：Robolectric／Roborazzi はメモリを使うのでヒープを広げる。
// android-all の取得方法（オフライン種まき）は run-tests.sh 側で指定する
tasks.withType<Test>().configureEach {
    maxHeapSize = "1536m"
}

// このVMでは Gradle の Test タスク自体が結果集計で落ちる（既知）ため、テストは JUnitCore で直接回す。
// その実行用クラスパスを取り出すためのタスク（docs/VERIFICATION.md 参照）
tasks.register("printUnitTestClasspath") {
    // NOTE: Test タスクのクラスパスを inputs.files に配線すると、Gradle が testDebugUnitTest 自体を
    // タスクグラフへ引き込み、このVMでは動かない Gradle Test Executor が走ってしまう。
    // コンパイルタスクへだけ依存し、クラスパスは doLast で遅延読みする（QRいっき読みと同方式）。
    // test_config.properties は、run-tests.sh が設定生成タスクをコマンドラインから明示実行して用意する
    // 本体クラスは runtime_app_classes_jar の classes.jar 経由でクラスパスに入る。その生成タスクは
    // Test タスクの依存にしかおらず、明示しないと jar が古いまま／無いままになり実行時に
    // NoSuchMethodError／NoClassDefFoundError になる（2026-11-24 v2.3.0 で踏んだ）
    dependsOn("compileDebugUnitTestKotlin", "bundleDebugClassesToRuntimeJar")
    val testTask = tasks.named<Test>("testDebugUnitTest")
    val out = layout.buildDirectory.file("unitTestClasspath.txt")
    outputs.file(out)
    doLast {
        out.get().asFile.writeText(testTask.get().classpath.files.joinToString(File.pathSeparator) { it.absolutePath })
        println("Wrote ${out.get().asFile.absolutePath}")
    }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.core:core-ktx:1.15.0")
    // 端末内AI（Gemini Nano）への質問用。対応端末でのみ動く
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta4")
    // JavaScript実行用（解釈モードで動かす）
    implementation("org.mozilla:rhino:1.7.15")
    testImplementation("junit:junit:4.13.2")
    // 実機なし検証（標準 2026-11-23）：JVM上で画面の見た目と遷移を交付前に確かめる
    testImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.35.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.35.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")
}
