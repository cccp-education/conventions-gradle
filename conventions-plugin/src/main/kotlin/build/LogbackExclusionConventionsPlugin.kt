package build

import org.gradle.api.Plugin
import org.gradle.api.Project

open class LogbackExclusionConventionsPlugin : Plugin<Project> {

    companion object {
        const val LOGBACK_GROUP = "ch.qos.logback"
        const val LOGBACK_MODULE = "logback-classic"

        val EXCLUDED_CONFIGURATIONS = listOf(
            "testRuntimeClasspath",
            "testImplementation",
            "functionalTestRuntimeClasspath"
        )
    }

    override fun apply(project: Project) {
        project.configurations.configureEach { config ->
            if (config.name in EXCLUDED_CONFIGURATIONS) {
                config.exclude(mapOf("group" to LOGBACK_GROUP, "module" to LOGBACK_MODULE))
            }
        }
    }
}