import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val signalKotlinJvmTarget: String by rootProject.extra

buildscript {
  rootProject.extra["kotlin_version"] = "1.8.10"

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
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlin_version"] as String}")
    classpath(libs.ktlint)
    classpath("com.squareup.wire:wire-gradle-plugin:4.4.3") {
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

val lib_signal_service_version_number: String by rootProject.extra
val lib_signal_service_group_info: String by rootProject.extra

allprojects {
  version = lib_signal_service_version_number
  group = lib_signal_service_group_info

  // Needed because otherwise the kapt task defaults to jvmTarget 17, which "poisons the well" and requires us to bump up too
  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      jvmTarget = signalKotlinJvmTarget
    }
  }
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
