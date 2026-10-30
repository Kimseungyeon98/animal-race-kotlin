import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 출시 서명 키: 저장소 밖에 두고 keystore.properties(커밋 안 함)로만 알려 준다.
// 파일이 없으면 release도 디버그 키로 서명 (내 폰 설치용, 스토어 업로드 불가)
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    // 코드 패키지는 그대로, 스토어용 앱 ID만 바꿈 (5.0)
    namespace = "com.example.animalrace"
    compileSdk = 36

    defaultConfig {
        // 구글 플레이 앱 ID: 출시 후에는 절대 바꿀 수 없음
        applicationId = "com.animalrace.android"
        minSdk = 24
        targetSdk = 36
        versionCode = 87
        versionName = "5.0"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) create("release") {
            storeFile = file(keystoreProps.getProperty("storeFile"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")

    testImplementation("junit:junit:4.13.2")
}
