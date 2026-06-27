package education.cccp.build

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
    }

    private fun configureJava(project: Project) {
        val java = project.extensions.getByType(JavaPluginExtension::class.java)
        java.sourceCompatibility = JavaVersion.VERSION_24
        java.targetCompatibility = JavaVersion.VERSION_24
        java.withSourcesJar()
        java.withJavadocJar()
    }

    private fun configureKotlin(project: Project) {
        val kotlinExt = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        kotlinExt.jvmToolchain(24)
    }

    private fun configureRepositories(project: Project) {
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
}
