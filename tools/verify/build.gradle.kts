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
        kotlin.exclude("**/MainActivity.kt", "**/data/PrefsRepository.kt", "**/ui/theme/Fonts.kt")
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

