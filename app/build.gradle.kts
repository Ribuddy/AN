plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "net.ritirp.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.ritirp.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // OpenGL ES 관련 설정
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }

    buildTypes {
        debug {
            // 디버그 모드에서 GL 에러 로깅 최소화
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            // 릴리스 모드에서 GL 최적화
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    // 패키징 옵션으로 중복 리소스 제거
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    android.set(true)
    ignoreFailures.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        include("**/kotlin/**")
    }
    // disabledRules 대신 .editorconfig 파일 사용 또는 여기서 직접 설정
}

// .editorconfig 파일을 생성하여 규칙 관리
tasks.register("createEditorConfig") {
    doLast {
        val editorConfig = file("${project.rootDir}/.editorconfig")
        if (!editorConfig.exists()) {
            editorConfig.writeText("""
                [*.{kt,kts}]
                ktlint_standard_function-naming = disabled
                ktlint_standard_no-wildcard-imports = disabled
                ktlint_standard_comment-wrapping = disabled
                ktlint_standard_discouraged-comment-location = disabled
                ktlint_standard_max-line-length = disabled
            """.trimIndent())
            println("Created .editorconfig file")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Material Icons Extended - 확장 아이콘 지원
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    // 카카오맵 SDK
    implementation("com.kakao.maps.open:android:2.11.9")

    // 위치 서비스
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // 권한 처리
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // 네비게이션
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // HTTP 클라이언트
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
