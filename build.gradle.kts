@file:Suppress("SpellCheckingInspection", "DEPRECATION")

plugins {
	java
	id("com.github.johnrengelman.shadow") version "+"
	id("com.github.breadmoirai.github-release") version "+"
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
tasks.named("githubRelease") { dependsOn(tasks.build) }
githubRelease {
	token(System.getenv("GITHUB_TOKEN"))
	owner = e("github.owner")
	repo = e("github.repo")
	tagName = "v${e("app.version")}"
	releaseName = tagName
	body =
		"**Full Changelog**: https://github.com/${e("github.owner")}/${e("github.repo")}/compare/v${getPreviousVersion()}...v${e("app.version")}"
	prerelease = true
	releaseAssets(tasks.shadowJar.get().outputs.files)
	overwrite = true // 如果标签已存在，将覆盖它
}
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
		val javaPath = javaToolchains.launcherFor(java.toolchain).get().metadata.installationPath.toString()
		val modules = providers.exec {
			commandLine(
				"$javaPath/bin/jdeps",
				"--multi-release", "17",
				"--print-module-deps",
				"--ignore-missing-deps",
				"$inputDir/$jarName"
			)
		}.standardOutput.asText.get().trim()
		println("依赖模块: $modules")
		println("正在使用jlink构建自定义运行时...")
		exec {
			commandLine(
				"$javaPath/bin/jlink",
				"--module-path", "$javaPath/jmods",
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
				"$javaPath/bin/jpackage",
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
fun getPreviousVersion(): String {
	val currentVersion = e("app.version")
	val versionParts = currentVersion.split(".")
	if(versionParts.size >= 3) {
		val major = versionParts[0].toInt()
		val minor = versionParts[1].toInt()
		val patch = versionParts[2].toInt()
		// 如果patch版本大于0，则递减patch版本
		if(patch > 0) return "$major.$minor.${patch - 1}"
		// 如果minor版本大于0，则递减minor版本，patch设为9
		else if(minor > 0) return "$major.${minor - 1}.9"
		// 如果major版本大于0，则递减major版本，minor设为9，patch设为9
		else if(major > 0) return "${major - 1}.9.9"
	}
	// 如果无法解析版本号或者是0.0.0，则返回默认值
	return "0.0.0"
}
