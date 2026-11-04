package build

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `education.cccp.build.library` — build convention for the N0 **contract
 * libraries** (workspace-bom modules).
 *
 * A contract module is a plain Kotlin JVM library published to Maven Central
 * and consumed through `implementation(...)`; it must **not** apply
 * `java-gradle-plugin` (which [GradlePluginConventionsPlugin] is built around).
 * This convention removes the remaining duplication: Java 25 +
 * sources/javadoc jars, Kotlin toolchain, repositories, build cache, JUnit test
 * configuration and the workspace guards are configured once.
 *
 * Publishing is deliberately **not** included: the consumer also applies
 * `education.cccp.build.publishing`, which owns the POM identity and the
 * conditional signing (CNV-13.1).
 */
class LibraryConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        val extension = project.extensions.create(
            "libraryConventions",
            LibraryConventionsExtension::class.java
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
        JvmProjectConventions.registerGuards(project)
    }
}
