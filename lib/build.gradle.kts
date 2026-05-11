dependencies {
  val arrowVersion: String by rootProject.extra
  val kotestVersion: String by rootProject.extra

  api("io.arrow-kt:arrow-core:$arrowVersion")
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:$arrowVersion")
}

mavenPublishing {
  coordinates(artifactId = "functional-errors")
  pom {
    name = "functional-errors"
    description =
      "Typesafe functional error handling with emulated stack traces, built on Arrow's Either."
  }
}
