pluginManagement {
  plugins {
    kotlin("jvm") version "2.1.0"
    id("com.diffplug.spotless") version "7.0.2"
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.vanniktech.maven.publish") version "0.29.0"
  }
}

rootProject.name = "Functional Errors"

include("lib")

include("lib-kotest")
