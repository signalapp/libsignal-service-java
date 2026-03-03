dependencyResolutionManagement {
  repositories {
    mavenCentral()
    mavenLocal()
    maven {
      name = "SignalBuildArtifacts"
      url = uri("https://build-artifacts.signal.org/libraries/maven/")
      content {
        includeGroupByRegex("org\\.signal.*")
      }
    }
  }
  versionCatalogs {
    // libs.versions.toml is automatically registered.
    create("testLibs") {
      from(files("gradle/test-libs.versions.toml"))
    }
  }
}

include("signal-service-java")
project(":signal-service-java").projectDir = file("service")

include(":core:util-jvm")
project(":core:util-jvm").projectDir = file("core-util-jvm")

include(":core:models-jvm")
project(":core:models-jvm").projectDir = file("core-models-jvm")
