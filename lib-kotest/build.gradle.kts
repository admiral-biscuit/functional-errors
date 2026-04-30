val kotestVersion: String by rootProject.extra

dependencies {
  api(project(":lib"))
  api("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}

mavenPublishing {
  coordinates(artifactId = "functional-errors-kotest")
  pom {
    name = "functional-errors-kotest"
    description = "Kotest matchers for the functional-errors library."
  }
}
