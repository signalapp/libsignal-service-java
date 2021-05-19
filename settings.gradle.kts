dependencyResolutionManagement {
  repositories {
    mavenCentral()
    mavenLocal()
  }
}

include("signal-service-java")
project(":signal-service-java").projectDir = file("service")

include(":core-util-jvm")

apply(from = "dependencies.gradle.kts")
