/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
  id("java-test-fixtures")
  id("maven-publish")
  id("signing")
  id("idea")
  alias(libs.plugins.ktlint)
  id("com.squareup.wire")
}

java {
  withJavadocJar()
  withSourcesJar()
  sourceCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
  targetCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
}

tasks.withType<KotlinCompile>().configureEach {
  kotlin {
    compilerOptions {
      jvmTarget = JvmTarget.fromTarget(libs.versions.kotlinJvmTarget.get())
      freeCompilerArgs = listOf("-Xjvm-default=all")
      suppressWarnings = true
    }
  }
}

val sourceSets = extensions.getByName("sourceSets") as SourceSetContainer
sourceSets.named("main") {
  output.dir(
    mapOf("builtBy" to tasks.named("compileKotlin")),
    "$buildDir/classes/kotlin/main"
  )
}
sourceSets.named("test") {
  output.dir(
    mapOf("builtBy" to tasks.named("compileTestKotlin")),
    "$buildDir/classes/kotlin/test"
  )
}

tasks.withType<Jar>().configureEach {
  manifest {
    attributes("Automatic-Module-Name" to "com.github.turasa.signalservice")
  }
}

afterEvaluate {
  listOf(
    "runKtlintCheckOverMainSourceSet",
    "sourcesJar",
    "runKtlintFormatOverMainSourceSet",
    "sourcesJar"
  ).forEach { taskName ->
    tasks.named(taskName) {
      mustRunAfter(tasks.named("generateMainProtos"))
    }
  }
}

ktlint {
  version.set("1.5.0")

  filter {
    exclude { entry ->
      entry.file.toString().contains("build/generated/source/wire")
    }
  }
}

wire {
  protoLibrary = true

  kotlin {
    javaInterop = true
  }

  sourcePath {
    srcDir("src/main/protowire")
  }

  custom {
    // Comes from wire-handler jar project
    schemaHandlerFactoryClass = "org.signal.wire.Factory"
  }
}

tasks.whenTaskAdded {
  if (name == "lint") {
    enabled = false
  }
}

dependencies {
  api(libs.google.libphonenumber)
  api(libs.jackson.core)
  api(libs.jackson.module.kotlin)

  api(libs.libsignal.client)
  api(libs.square.okhttp3)
  api(libs.square.okio)
  implementation(libs.google.jsr305)

  api(libs.rxjava3.rxjava)
  implementation(libs.rxjava3.rxkotlin)

  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)

  api(project(":core:util-jvm"))
  api(project(":core:models-jvm"))

  testImplementation(testLibs.junit.junit)
  testImplementation(testLibs.assertk)
  testImplementation(testLibs.conscrypt.openjdk.uber)
  testImplementation(testLibs.mockk)

  testFixturesImplementation(libs.libsignal.client)
  testFixturesImplementation(testLibs.junit.junit)
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])

      pom {
        name.set("signal-service-java")
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
