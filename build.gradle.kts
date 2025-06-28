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
	implementation("com.formdev:flatlaf:+")
	implementation("com.formdev:flatlaf-intellij-themes:+")
	implementation("com.formdev:flatlaf-extras:+")
	implementation("info.picocli:picocli:+")
	implementation("org.json:json:+")
	implementation("it.unimi.dsi:fastutil:+")
}
githubRelease {
	token(System.getenv("GITHUB_TOKEN"))
	owner = "ForgeStove"
	repo = "HexSync"
	tagName = "v${p("app.version")}"
	releaseName = tagName
	generateReleaseNotes = true
	prerelease = true
	releaseAssets(tasks.shadowJar.get().outputs.files)
	overwrite = true
}
tasks.shadowJar {
	archiveClassifier.set("")
	manifest { attributes(mapOf("Main-Class" to "com.forgestove.hexsync.HexSync")) }
	mergeServiceFiles()
	minimize {
		exclude(dependency("com.formdev:flatlaf-intellij-themes"))
	}
	from("LICENSE") { rename { "${it}_${project.name}" } }
}
tasks.register("packageApp") {
	group = tasks.shadowJar.get().group
	description = "打包应用程序为可执行镜像"
	dependsOn(tasks.shadowJar)
	doLast {
		val jarName = p("app.jarName")
		val inputDir = p("app.inputDir")
		val outputDir = p("app.outputDir")
		val runtimeDir = p("app.runtimeDir")
		delete(outputDir)
		println("已删除旧的输出目录: $outputDir")
		delete(runtimeDir)
		println("已删除旧的运行时目录: $runtimeDir")
		println("正在分析依赖模块...")
		val javaPath = javaToolchains.launcherFor(java.toolchain).get().metadata.installationPath.toString()
		val modules = providers.exec {
			commandLine(
				"$javaPath/bin/jdeps", "--multi-release", "17", "--print-module-deps", "--ignore-missing-deps", "$inputDir/$jarName"
			)
		}.standardOutput.asText.get().trim()
		println("依赖模块: $modules")
		println("正在使用jlink构建自定义运行时...")
		exec {
			commandLine(
				"$javaPath/bin/jlink",
				"--module-path",
				"$javaPath/jmods",
				"--add-modules",
				modules,
				"--output",
				runtimeDir,
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
				"--input",
				inputDir,
				"--name",
				"HexSync",
				"--main-jar",
				jarName,
				"--main-class", p("app.mainClass"),
				"--icon", p("app.iconFile"),
				"--type",
				"app-image",
				"--runtime-image",
				runtimeDir,
				"--dest",
				outputDir,
				"--vendor", p("app.vendor"),
				"--description", p("app.description"),
				"--copyright", p("app.copyright"),
				"--app-version", p("app.version")
			)
		}
		println("应用已打包到: $outputDir")
		delete(runtimeDir)
		println("已删除临时运行时目录: $runtimeDir")
		delete("$outputDir/HexSync/HexSync.ico")
		println("已删除多余的图标文件")
	}
}
fun p(key: String) = property(key).toString()
