dependencyResolutionManagement {
  repositories {
    mavenCentral()
    mavenLocal()
  }
}

include("libsignal-service")
project(":libsignal-service").projectDir = file("service")

include(":core-util-jvm")

apply(from = "dependencies.gradle.kts")
