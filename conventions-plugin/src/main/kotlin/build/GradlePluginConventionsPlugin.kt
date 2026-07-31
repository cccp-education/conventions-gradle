package build

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class GradlePluginConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("java-gradle-plugin")
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        val extension = project.extensions.create(
            "gradlePluginConventions",
            GradlePluginConventionsExtension::class.java
        )

        configureJava(project)
        configureKotlin(project)
        configureRepositories(project)
        configureBuildCache(project)
        configureTestTasks(project, extension)
        configureTestDependencies(project)
        configureAnnotationsConflict(project, extension)
    }

    private fun configureJava(project: Project) {
        val java = project.extensions.getByType(JavaPluginExtension::class.java)
        java.sourceCompatibility = JavaVersion.VERSION_25
        java.targetCompatibility = JavaVersion.VERSION_25
        java.withSourcesJar()
        java.withJavadocJar()
    }

    private fun configureKotlin(project: Project) {
        val kotlinExt = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        kotlinExt.jvmToolchain(25)
    }

    private fun configureRepositories(project: Project) {
        project.repositories.mavenLocal()
        project.repositories.mavenCentral()
        project.repositories.gradlePluginPortal()
    }

    private fun configureBuildCache(project: Project) {
        project.gradle.startParameter.isBuildCacheEnabled = true
    }

    private fun configureTestTasks(project: Project, extension: GradlePluginConventionsExtension) {
        project.tasks.withType(Test::class.java).configureEach(
            Action { test ->
                test.useJUnitPlatform()
                test.testLogging(
                    Action { logging ->
                        logging.events = setOf(
                            TestLogEvent.PASSED,
                            TestLogEvent.SKIPPED,
                            TestLogEvent.FAILED
                        )
                        logging.showStandardStreams = true
                    }
                )
                if (extension.enableDynamicAgentLoading) {
                    test.jvmArgs("-XX:+EnableDynamicAgentLoading")
                }
                extension.maxHeapSize?.let { heap ->
                    test.maxHeapSize = heap
                }
                if (extension.parallelExecution) {
                    test.systemProperty("junit.jupiter.execution.parallel.enabled", "true")
                }
            }
        )
    }

    // CNV-7.2 — Centralise les deps junit test + platform BOM (tue le bug 6.0.3 plantuml)
    private fun configureTestDependencies(project: Project) {
        project.afterEvaluate {
            TestDependencies.addPlatformBom(project, "testImplementation")
            TestDependencies.addPlatformBom(project, "testRuntimeOnly")
            TestDependencies.addJunitDeps(project, "testImplementation", "testRuntimeOnly")
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
