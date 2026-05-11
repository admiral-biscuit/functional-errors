plugins { application }

dependencies {
  val arrowVersion: String by rootProject.extra
  val kotestVersion: String by rootProject.extra

  implementation(project(":lib"))

  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:$arrowVersion")
  testImplementation(project(":lib-kotest"))
}

application { mainClass.set("io.github.admiralbiscuit.functionalerrors.examples.MainKt") }
