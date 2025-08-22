plugins {
	java
	id("com.github.johnrengelman.shadow") version "+"
	id("com.github.breadmoirai.github-release") version "+"
	id("edu.sc.seis.launch4j") version "+"
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
launch4j {
	mainClassName = p("app_mainClass")
	outfile = "${p("app_name")}-${p("app_version")}.exe"
	outputDir = "launch4j"
	jarFiles = tasks.shadowJar.get().outputs.files
	icon = "${rootDir}/icon.ico"
	jvmOptions = listOf("-Xms256m", "-Xmx1024m")
//	bundledJrePath = "jre"
	jreMinVersion = "17"
	windowTitle = p("app_name")
	version= p("app_version")
	textVersion = p("app_version")
	copyright = p("app_copyright")
}
githubRelease {
	token(System.getenv("GITHUB_TOKEN"))
	owner = p("app_author")
	repo = p("app_name")
	tagName = "v${p("app_version")}"
	releaseName = tagName
	generateReleaseNotes = true
	prerelease = true
	releaseAssets(tasks.shadowJar.get().outputs.files)
	overwrite = true
}
tasks.shadowJar {
	archiveClassifier.set("")
	manifest { attributes(mapOf("Main-Class" to p("app_mainClass"))) }
	mergeServiceFiles()
	minimize {
		exclude(dependency("com.formdev:flatlaf-intellij-themes"))
	}
	from("LICENSE") { rename { "${it}_${project.name}" } }
}
fun p(key: String) = property(key).toString()
