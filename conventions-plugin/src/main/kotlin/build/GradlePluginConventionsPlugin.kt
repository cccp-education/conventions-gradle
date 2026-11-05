package build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.plugin.compatibility.compatibility
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension

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
        configurePluginCompatibility(project)
        JvmProjectConventions.registerGuards(project)
    }

    /**
     * Gradle Plugin Portal compatibility metadata (ACADEMY-16 / S-027).
     *
     * The compatibility extension is only present when the consumer applies
     * the Plugin Publish plugin 2.1.0+ (which auto-applies
     * `org.gradle.plugin-compatibility`). The `withPlugin` hook makes the
     * default independent of the order of the consumer's `plugins {}` block.
     *
     * The default is **false**: declaring an unsupported feature is honest and
     * recommended by the Portal, while a false positive can have the badge
     * revoked. A consumer that has proven the feature can override it in its
     * own plugin declaration.
     *
     * Only `configurationCache` is set here: it exists since the compatibility
     * plugin 1.0.0, whereas `isolatedProjects` requires 1.1.0 (Plugin Publish
     * 2.2.1). A consumer on Plugin Publish 2.1.0 would otherwise hit a
     * `NoSuchMethodError` at runtime.
     */
    private fun configurePluginCompatibility(project: Project) {
        project.pluginManager.withPlugin("org.gradle.plugin-compatibility") {
            project.extensions.configure(GradlePluginDevelopmentExtension::class.java) { gradlePlugin ->
                gradlePlugin.plugins.all { declaration ->
                    declaration.compatibility { compatibility ->
                        compatibility.features { features ->
                            features.configurationCache.set(false)
                        }
                    }
                }
            }
        }
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
