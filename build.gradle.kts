import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.plugins.JavaPluginExtension

plugins {
  kotlin("jvm") apply false
  id("com.diffplug.spotless") apply false
}

val kotestVersion by extra("5.9.1")

subprojects {
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "java-library")
  apply(plugin = "com.diffplug.spotless")
  apply(plugin = "maven-publish")

  group = "io.github.admiral-biscuit"
  version = "0.1.0"

  repositories { mavenCentral() }

  extensions.configure<JavaPluginExtension> {
    withSourcesJar()
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
  }

  tasks.withType<Test> { useJUnitPlatform() }

  extensions.configure<SpotlessExtension> {
    kotlin {
      licenseHeader("// SPDX-License-Identifier: MIT-0")
      ktfmt().googleStyle()
    }
  }
}
