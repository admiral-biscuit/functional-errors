import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.plugins.JavaPluginExtension

plugins {
  kotlin("jvm") apply false
  id("com.diffplug.spotless") apply false
  id("org.jetbrains.dokka") apply false
  id("com.vanniktech.maven.publish") apply false
}

val kotestVersion by extra("5.9.1")

subprojects {
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "java-library")
  apply(plugin = "com.diffplug.spotless")
  apply(plugin = "org.jetbrains.dokka")
  apply(plugin = "com.vanniktech.maven.publish")

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

  extensions.configure<MavenPublishBaseExtension> {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    pom {
      url = "https://github.com/admiral-biscuit/functional-errors"
      licenses {
        license {
          name = "MIT No Attribution"
          url = "https://opensource.org/licenses/MIT-0"
          distribution = "repo"
        }
      }
      developers {
        developer {
          id = "admiral-biscuit"
          name = "Jordi Kling"
          url = "https://github.com/admiral-biscuit"
        }
      }
      scm {
        url = "https://github.com/admiral-biscuit/functional-errors"
        connection = "scm:git:git://github.com/admiral-biscuit/functional-errors.git"
        developerConnection =
          "scm:git:ssh://github.com/admiral-biscuit/functional-errors.git"
      }
    }
  }
}
