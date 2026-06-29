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

nmcpSettings {
    centralPortal {
        username = globalProps.getProperty("ossrhUsername") ?: error("ossrhUsername not found")
        password = globalProps.getProperty("ossrhPassword") ?: error("ossrhPassword not found")
        publishingType = "AUTOMATIC"
    }
}

rootProject.name = "conventions-plugin"
