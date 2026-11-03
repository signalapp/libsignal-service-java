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

include(":lib:libsignal-service")

include(":lib:network")

include(":core:util-jvm")

include(":core:network")

include(":core:serialization")
