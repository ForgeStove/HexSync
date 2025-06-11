@file:Suppress("SpellCheckingInspection")

import java.io.ByteArrayOutputStream

plugins {
	java
	id("com.github.johnrengelman.shadow") version "+"
}
java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
repositories {
	mavenLocal()
	mavenCentral()
}
dependencies {
	compileOnly("org.jetbrains:annotations:+")
	implementation("com.formdev:flatlaf:1.1.1")
	implementation("info.picocli:picocli:+")
}
tasks.shadowJar {
	archiveClassifier.set("")
	manifest { attributes(mapOf("Main-Class" to "com.forgestove.hexsync.HexSync")) }
	mergeServiceFiles()
	minimize {
		exclude(dependency("com.formdev:flatlaf"))
	}
	from("LICENSE")
}
tasks.build { dependsOn(tasks.shadowJar) }
// 自定义打包任务，替代build.sh脚本功能
tasks.register("packageApp") {
    description = "打包应用程序为可执行镜像"
    group = "distribution"
    dependsOn("build")
    doLast {
        val jarName = e("app.jarName")
        val inputDir = e("app.inputDir")
        val outputDir = e("app.outputDir")
        val runtimeDir = e("app.runtimeDir")
        delete(outputDir)
        println("已删除旧的输出目录: $outputDir")
        delete(runtimeDir)
        println("已删除旧的运行时目录: $runtimeDir")
        println("正在分析依赖模块...")
        val outputStream = ByteArrayOutputStream()
        exec {
            commandLine(
                "${System.getProperty("java.home")}/bin/jdeps",
                "--multi-release", "17",
                "--print-module-deps",
                "--ignore-missing-deps",
                "$inputDir/$jarName"
            )
            isIgnoreExitValue = true
            standardOutput = outputStream
        }
        val modules = outputStream.toString().trim()
        println("依赖模块: $modules")
        println("正在使用jlink构建自定义运行时...")
        exec {
            commandLine(
                "${System.getProperty("java.home")}/bin/jlink",
                "--module-path", "${System.getProperty("java.home")}/jmods",
                "--add-modules", modules,
                "--output", runtimeDir,
                "--strip-debug",
                "--compress=2",
                "--no-header-files",
                "--no-man-pages"
            )
        }
        println("自定义运行时已生成: $runtimeDir")
        println("正在使用jpackage打包应用...")
        exec {
            commandLine(
                "${System.getProperty("java.home")}/bin/jpackage",
                "--input", inputDir,
                "--name", "HexSync",
                "--main-jar", jarName,
                "--main-class", e("app.mainClass"),
                "--icon", e("app.iconFile"),
                "--type", "app-image",
                "--runtime-image", runtimeDir,
                "--dest", outputDir,
                "--vendor", e("app.vendor"),
                "--description", e("app.description"),
                "--copyright", e("app.copyright"),
                "--app-version", e("app.version")
            )
        }
        println("应用已打包到: $outputDir")
        delete(runtimeDir)
        println("已删除临时运行时目录: $runtimeDir")
        delete("$outputDir/HexSync/HexSync.ico")
        println("已删除多余的图标文件")
    }
}
fun e(key: String) = extra[key].toString()
