plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
}
// 앱 소스를 그대로 가져다 데스크톱 Compose로 컴파일한다 (Android SDK 없이 검사용)
val app = rootDir.resolve("../../app/src").normalize()
val src = app.resolve("main/java").path
sourceSets {
    test {
        kotlin.setSrcDirs(listOf(app.resolve("test/java").path))
    }
    main {
        kotlin.setSrcDirs(listOf(src, "stub", "sim"))
        kotlin.exclude("**/MainActivity.kt", "**/data/PrefsRepository.kt", "**/data/AndroidSound.kt", "**/data/AndroidSharer.kt", "**/ui/theme/Fonts.kt")
    }
}
kotlin { jvmToolchain(21) }
dependencies {
    val cmp = "1.7.3"
    implementation("org.jetbrains.compose.runtime:runtime-desktop:$cmp")
    implementation("org.jetbrains.compose.foundation:foundation-desktop:$cmp")
    implementation("org.jetbrains.compose.material3:material3-desktop:1.7.3")
    implementation("org.jetbrains.compose.material:material-icons-core-desktop:1.7.3")
    implementation("org.jetbrains.compose.ui:ui-desktop:$cmp")
    testImplementation("junit:junit:4.13.2")
    // 스크린샷(shot)용 그림 엔진 네이티브 (리눅스)
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.8.18")
}
tasks.register<JavaExec>("sim") {
    classpath = sourceSets["main"].output + configurations["compileClasspath"]
    mainClass.set("com.example.animalrace.race.StorySimKt")
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dfile.encoding=UTF-8")
}

tasks.test {
    classpath = sourceSets["test"].output + sourceSets["main"].output + configurations["testCompileClasspath"]
    testLogging { events("passed", "failed"); showStandardStreams = true }
}

// 앱 화면 PNG (스크린샷): ./gradlew shot [-Pscene=이름] [-Pout=폴더]  → 기본 build/shots
tasks.register<JavaExec>("shot") {
    classpath = sourceSets["main"].output + configurations["runtimeClasspath"]
    mainClass.set("com.example.animalrace.shot.ShotKt")
    args(project.findProperty("out") ?: layout.buildDirectory.dir("shots").get().asFile.path, project.findProperty("scene") ?: "all")
    jvmArgs("-Djava.awt.headless=true", "-Danimalrace.fonts=" + app.resolve("main/res/font").path, "-Dfile.encoding=UTF-8")
}
