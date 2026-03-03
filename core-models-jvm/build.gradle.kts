/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.ktlint)
  id("maven-publish")
  id("signing")
}

ktlint {
  version.set("1.5.0")
}

java {
  withJavadocJar()
  withSourcesJar()
  sourceCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
  targetCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
}

kotlin {
  jvmToolchain {
    languageVersion = JavaLanguageVersion.of(libs.versions.kotlinJvmTarget.get())
  }
}

dependencies {
  implementation(libs.libsignal.client)
  implementation(libs.square.okio)
  implementation(project(":core:util-jvm"))
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])

      pom {
        name.set("core-models")
        description.set("Signal Service communication library for Java, unofficial fork")
        url.set("https://github.com/Turasa/libsignal-service-java")
        licenses {
          license {
            name.set("GPLv3")
            url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
          }
        }
        developers {
          developer {
            name.set("Moxie Marlinspike")
          }
          developer {
            name.set("Sebastian Scheibner")
          }
          developer {
            name.set("Tilman Hoffbauer")
          }
        }
        scm {
          connection.set("scm:git@github.com:Turasa/libsignal-service-java.git")
          developerConnection.set("scm:git@github.com:Turasa/libsignal-service-java.git")
          url.set("scm:git@github.com:Turasa/libsignal-service-java.git")
        }
      }
    }
  }
}

signing {
  isRequired = gradle.taskGraph.hasTask("uploadArchives")
  sign(publishing.publications["mavenJava"])
}
