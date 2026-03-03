import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.jetbrains.kotlin.jvm) apply false
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

buildscript {
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://plugins.gradle.org/m2/")
      content {
        includeGroupByRegex("org\\.jlleitschuh\\.gradle.*")
      }
    }
  }
  dependencies {
    classpath("com.squareup.wire:wire-gradle-plugin:4.9.11") {
      exclude(group = "com.squareup.wire", module = "wire-swift-generator")
      exclude(group = "com.squareup.wire", module = "wire-grpc-client")
      exclude(group = "com.squareup.wire", module = "wire-grpc-jvm")
      exclude(group = "com.squareup.wire", module = "wire-grpc-server-generator")
      exclude(group = "io.outfoxx", module = "swiftpoet")
    }
    classpath(files("$rootDir/wire-handler/wire-handler-1.0.0.jar"))
  }
}

apply(from = "${rootDir}/constants.gradle.kts")

project.group = "com.github.turasa"

val repositoryUsername = project.findProperty("whisperSonatypeUsername")?.toString() ?: ""
val repositoryPassword = project.findProperty("whisperSonatypePassword")?.toString() ?: ""

nexusPublishing {
  repositories {
    sonatype {
      username = repositoryUsername
      password = repositoryPassword

      // central portal URLs
      nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
      snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
    }
  }
}

val lib_signal_service_version_number: String by rootProject.extra
val lib_signal_service_group_info: String by rootProject.extra

allprojects {
  version = lib_signal_service_version_number
  group = lib_signal_service_group_info

  tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
}

subprojects {
  if (JavaVersion.current().isJava8Compatible) {
    allprojects {
      tasks.withType<Javadoc>() {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
      }
    }
  }
}
