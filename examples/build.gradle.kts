plugins {
  application
}

dependencies {
  implementation(project(":lib"))
}

application {
  mainClass.set("io.github.admiralbiscuit.functionalerrors.examples.MainKt")
}
