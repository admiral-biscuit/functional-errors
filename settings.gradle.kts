pluginManagement {
  plugins {
    kotlin("jvm") version "2.1.0"
    id("com.diffplug.spotless") version "7.0.2"
  }
}

rootProject.name = "Functional Errors"

include("lib")

include("lib-kotest")
