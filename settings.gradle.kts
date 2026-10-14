import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProperties = Properties()
val localPropertiesFile = file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
// Credentials for the Light keyboard package (GitHub Packages) come from the
// gitignored local.properties (gpr.user / gpr.key), GH_PACKAGES_* env vars, or
// the machine-level ~/.gradle/gradle.properties.
val ghUsername = localProperties.getProperty("gpr.user")
    ?: System.getenv("GH_PACKAGES_USER")
    ?: providers.gradleProperty("gpr.user").orNull
val ghPassword = localProperties.getProperty("gpr.key")
    ?: System.getenv("GH_PACKAGES_TOKEN")
    ?: providers.gradleProperty("gpr.key").orNull

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // The Light keyboard library is a transitive dependency of :sdk:ui. Some
        // SDK revisions publish it to JitPack (com.github.lightphone:light-keyboard),
        // others to GitHub Packages (com.thelightphone.lp3keyboard:ui). Declare both.
        maven {
            name = "JitPack"
            url = uri("https://jitpack.io")
        }
        maven {
            name = "GitHubPackages-Keyboard"
            url = uri("https://maven.pkg.github.com/lightphone/light-keyboard")
            credentials {
                username = ghUsername
                password = ghPassword
            }
        }
    }
    // Reuse the SDK's own version catalog so this shell never drifts from
    // whatever the pinned submodule expects.
    versionCatalogs {
        create("libs") {
            from(files("light-sdk/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "wikipedia"

// The `light.sdk` Gradle plugin lives in the submodule and is contributed as
// an included build (matches the SDK monorepo's own includeBuild("plugin")).
includeBuild("light-sdk/plugin")

// SDK library projects, sourced from the submodule. :sdk:client transitively
// needs :sdk:ui, :sdk:shared and :lint-rules.
include(":lint-rules")
project(":lint-rules").projectDir = file("light-sdk/lint-rules")
include(":sdk")
project(":sdk").projectDir = file("light-sdk/sdk")
include(":sdk:shared")
project(":sdk:shared").projectDir = file("light-sdk/sdk/shared")
include(":sdk:ui")
project(":sdk:ui").projectDir = file("light-sdk/sdk/ui")
include(":sdk:client")
project(":sdk:client").projectDir = file("light-sdk/sdk/client")

// The tool itself -- the only dev-owned module, and the only thing Light's
// build/review pipeline extracts (tool/build.gradle.kts, tool/lighttool.toml,
// tool/src/main/**).
include(":tool")
