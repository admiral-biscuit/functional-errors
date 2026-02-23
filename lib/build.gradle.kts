plugins {
  kotlin("jvm") version "2.1.0"
  `java-library`
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

repositories { mavenCentral() }

dependencies {
  api("io.arrow-kt:arrow-core:2.0.0")
  val kotestVersion = "5.9.1"
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:2.0.0")
}

tasks.named<Test>("test") { useJUnitPlatform() }
