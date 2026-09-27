pluginManagement.repositories.mavenLocal()
pluginManagement.repositories.mavenCentral()
pluginManagement.repositories.gradlePluginPortal()

plugins {
    id("com.gradleup.nmcp.settings").version("1.5.0")
}

val globalProps = java.util.Properties().also {
    val globalFile = file(System.getProperty("user.home") + "/.gradle/gradle.properties")
    if (globalFile.exists()) it.load(globalFile.inputStream())
}

// Credentials are only needed to publish to Maven Central. A CI runner has no
// ~/.gradle/gradle.properties, so a hard `error(...)` would abort configuration
// before any test runs. Fall back to empty strings, exactly like the sibling
// workspace-bom settings; publishing still fails loudly if credentials are absent.
nmcpSettings {
    centralPortal {
        username = globalProps.getProperty("ossrhUsername") ?: ""
        password = globalProps.getProperty("ossrhPassword") ?: ""
        publishingType = "AUTOMATIC"
    }
}

rootProject.name = "conventions-plugin"
