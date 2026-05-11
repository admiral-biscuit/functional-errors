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

val arrowVersion by extra("2.0.0")
val kotestVersion by extra("5.9.1")

subprojects {
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "com.diffplug.spotless")

  group = "io.github.admiral-biscuit"
  version = "0.2.0"

  repositories { mavenCentral() }

  extensions.configure<JavaPluginExtension> {
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

configure(subprojects.filter { it.name != "examples" }) {
  apply(plugin = "java-library")
  apply(plugin = "org.jetbrains.dokka")
  apply(plugin = "com.vanniktech.maven.publish")

  extensions.configure<JavaPluginExtension> { withSourcesJar() }

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
        developerConnection = "scm:git:ssh://github.com/admiral-biscuit/functional-errors.git"
      }
    }
  }
}
