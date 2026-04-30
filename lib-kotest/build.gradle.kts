val kotestVersion: String by rootProject.extra

dependencies {
  api(project(":lib"))
  api("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])

      groupId = project.group.toString()
      artifactId = "functional-errors-kotest"
      version = project.version.toString()
    }
  }
}
