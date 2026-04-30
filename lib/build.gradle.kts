dependencies {
  api("io.arrow-kt:arrow-core:2.0.0")
  val kotestVersion: String by rootProject.extra
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:2.0.0")
}

mavenPublishing {
  coordinates(artifactId = "functional-errors")
  pom {
    name = "functional-errors"
    description =
      "Typesafe functional error handling with emulated stack traces, built on Arrow's Either."
  }
}
