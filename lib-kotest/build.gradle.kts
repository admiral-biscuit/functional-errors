val kotestVersion: String by rootProject.extra

dependencies {
  api(project(":lib"))
  api("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}
