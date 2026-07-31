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

        configureJava(project)
        configureKotlin(project)
        configureRepositories(project)
        configureTestTasks(project)
        configureTestDependencies(project)
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

    private fun configureTestTasks(project: Project) {
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
}
