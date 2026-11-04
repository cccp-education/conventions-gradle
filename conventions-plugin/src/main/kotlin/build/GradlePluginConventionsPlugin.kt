package build

import org.gradle.api.Plugin
import org.gradle.api.Project

class GradlePluginConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("java-gradle-plugin")
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        val extension = project.extensions.create(
            "gradlePluginConventions",
            GradlePluginConventionsExtension::class.java
        )

        JvmProjectConventions.configureJava(project)
        JvmProjectConventions.configureKotlin(project)
        JvmProjectConventions.configureRepositories(project)
        JvmProjectConventions.configureBuildCache(project)
        JvmProjectConventions.configureTestTasks(project) {
            TestTaskOptions(
                extension.enableDynamicAgentLoading,
                extension.maxHeapSize,
                extension.parallelExecution
            )
        }
        JvmProjectConventions.configureTestDependencies(project)
        configureAnnotationsConflict(project, extension)
        JvmProjectConventions.registerGuards(project)
    }

    // CNV-12.2 — fixAnnotationsConflict (Famille 6 CODE_REVIEW_GLOBALE)
    private fun configureAnnotationsConflict(project: Project, extension: GradlePluginConventionsExtension) {
        if (extension.fixAnnotationsConflict) {
            project.buildscript.configurations.all { config ->
                config.resolutionStrategy.eachDependency { details ->
                    if (details.requested.group == "org.jetbrains" && details.requested.name == "annotations") {
                        details.useVersion("26.0.2-1")
                    }
                }
            }
        }
    }
}
