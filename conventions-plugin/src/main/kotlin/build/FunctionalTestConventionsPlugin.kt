package build

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension

open class FunctionalTestConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "functionalTestConventions",
            FunctionalTestConventionsExtension::class.java
        )

        project.afterEvaluate {
            configureFunctionalTest(project, extension)
        }
    }

    private fun configureFunctionalTest(project: Project, extension: FunctionalTestConventionsExtension) {
        val sourceSetName = extension.sourceSetName

        project.pluginManager.apply("java-base")

        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val ftSourceSet = sourceSets.create(sourceSetName) { ss: SourceSet ->
            ss.java.setSrcDirs(listOf("src/$sourceSetName/kotlin"))
            ss.resources.setSrcDirs(listOf("src/$sourceSetName/resources"))
        }

        val libs: VersionCatalog? = try {
            project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        } catch (_: Exception) {
            null
        }

        val implConfig = ftSourceSet.implementationConfigurationName
        addPlatformBom(project, implConfig)
        project.dependencies.add(implConfig, "org.gradle:gradle-testkit:${project.gradle.gradleVersion}")
        addFromCatalog(project, libs, implConfig, "junit-jupiter", "org.junit.jupiter:junit-jupiter:5.12.2")
        addFromCatalog(project, libs, implConfig, "assertj-core", "org.assertj:assertj-core:3.25.3")

        extension.additionalDependencies.forEach { dep ->
            project.dependencies.add(implConfig, dep)
        }

        val runtimeConfig = ftSourceSet.runtimeOnlyConfigurationName
        addPlatformBom(project, runtimeConfig)
        addFromCatalog(project, libs, runtimeConfig, "junit-platform-launcher", "org.junit.platform:junit-platform-launcher:1.14.3")

        val ftTask = project.tasks.register(sourceSetName, Test::class.java) { task ->
            task.testClassesDirs = ftSourceSet.output.classesDirs
            task.classpath = project.configurations.getByName(ftSourceSet.runtimeClasspathConfigurationName) + ftSourceSet.output
            task.useJUnitPlatform()
        }

        try {
            val gradlePlugin = project.extensions.getByType(GradlePluginDevelopmentExtension::class.java)
            gradlePlugin.testSourceSets.add(ftSourceSet)
        } catch (_: Exception) {
        }

        project.tasks.named("check") { checkTask ->
            checkTask.dependsOn(ftTask)
        }
    }

    private fun addPlatformBom(project: Project, config: String) {
        try {
            project.dependencies.add(config, project.dependencies.platform("education.cccp:workspace-bom:0.0.4"))
        } catch (_: Exception) {
        }
    }

    private fun addFromCatalog(project: Project, libs: VersionCatalog?, config: String, catalogKey: String, fallback: String) {
        if (libs != null) {
            val provider = libs.findLibrary(catalogKey)
            if (provider.isPresent) {
                project.dependencies.add(config, provider.get())
                return
            }
        }
        project.dependencies.add(config, fallback)
    }
}
