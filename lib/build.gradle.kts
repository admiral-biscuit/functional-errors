import java.net.URI

plugins {
  `maven-publish`
}

group = "io.github.admiral-biscuit"

version = "0.0.1"

dependencies {
  api("io.arrow-kt:arrow-core:2.0.0")
  val kotestVersion: String by rootProject.extra
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:2.0.0")
}

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
