@file:Suppress("SpellCheckingInspection")

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
	implementation("org.aeonbits.owner:owner:+")
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
