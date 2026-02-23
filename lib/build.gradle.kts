import java.net.URI

plugins {
  kotlin("jvm") version "2.1.0"
  `java-library`
  `maven-publish`
}

group = "io.github.admiral-biscuit"

version = "0.0.1"

java {
  withSourcesJar()
  toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

repositories { mavenCentral() }

dependencies {
  api("io.arrow-kt:arrow-core:2.0.0")
  val kotestVersion = "5.9.1"
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:2.0.0")
}

tasks.named<Test>("test") { useJUnitPlatform() }

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])

      groupId = project.group.toString()
      artifactId = "functional-errors"
      version = project.version.toString()

      pom {
        name = "functional-errors"
        artifactId = "functional-errors"
        description = "Functional error handling utilities"
        url = "https://github.com/admiral-biscuit/functional-errors"
      }
    }
  }

  repositories {
    maven {
      name = "GitHubPackages"
      url = URI("https://maven.pkg.github.com/admiral-biscuit/functional-errors")
      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }
}
