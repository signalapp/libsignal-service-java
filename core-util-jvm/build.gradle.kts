/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

val signalJavaVersion: JavaVersion by rootProject.extra

plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
  id("org.jlleitschuh.gradle.ktlint")
  id("maven-publish")
  id("signing")
}

java {
  withJavadocJar()
  withSourcesJar()
  sourceCompatibility = signalJavaVersion
  targetCompatibility = signalJavaVersion
}

dependencies {
  testImplementation(testLibs.junit.junit)
  testImplementation(testLibs.assertj.core)
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])

      pom {
        name.set("core-util-jvm")
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
