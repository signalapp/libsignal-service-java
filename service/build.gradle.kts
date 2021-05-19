import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
  id("java-test-fixtures")
  id("maven-publish")
  id("signing")
  id("idea")
  id("org.jlleitschuh.gradle.ktlint")
  id("com.squareup.wire")
}

val signalBuildToolsVersion: String by rootProject.extra
val signalCompileSdkVersion: String by rootProject.extra
val signalTargetSdkVersion: Int by rootProject.extra
val signalMinSdkVersion: Int by rootProject.extra
val signalJavaVersion: JavaVersion by rootProject.extra
val signalKotlinJvmTarget: String by rootProject.extra

java {
  withJavadocJar()
  withSourcesJar()
  sourceCompatibility = signalJavaVersion
  targetCompatibility = signalJavaVersion
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = signalKotlinJvmTarget
  }
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
    "runKtlintFormatOverMainSourceSet"
  ).forEach { taskName ->
    tasks.named(taskName) {
      mustRunAfter(tasks.named("generateMainProtos"))
    }
  }
}

ktlint {
  version.set("0.49.1")

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

  implementation(libs.kotlin.stdlib.jdk8)

  api(project(":core-util-jvm"))

  testImplementation(testLibs.junit.junit)
  testImplementation(testLibs.assertj.core)
  testImplementation(testLibs.conscrypt.openjdk.uber)
  testImplementation(testLibs.mockito.core)

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
