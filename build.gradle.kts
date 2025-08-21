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
	releaseAssets(tasks.build.get().outputs.files)
	overwrite = true
}
tasks.build {
	dependsOn(tasks.shadowJar)
}
tasks.shadowJar {
	archiveClassifier.set("")
	manifest { attributes(mapOf("Main-Class" to p("app.mainClass"))) }
	mergeServiceFiles()
	minimize {
		exclude(dependency("com.formdev:flatlaf-intellij-themes"))
	}
	from("LICENSE") { rename { "${it}_${project.name}" } }
}
fun p(key: String) = property(key).toString()
