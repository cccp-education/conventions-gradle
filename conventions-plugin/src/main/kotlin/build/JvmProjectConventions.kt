package build

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * JVM project configuration shared by the convention plugins.
 *
 * Factorised at the **second** consumer (amendement pilote S-195):
 * [GradlePluginConventionsPlugin] (a published plugin project) and
 * [LibraryConventionsPlugin] (an N0 library) configure Java 25, Kotlin, the
 * repositories, the test tasks and the test dependencies identically. Keeping
 * one implementation is what makes the two conventions provably consistent.
 *
 * The object only configures an already-applied project: it never applies a
 * plugin itself, so each caller keeps control of its plugin set
 * (`java-gradle-plugin` vs `java-library`).
 */
internal object JvmProjectConventions {

    fun configureJava(project: Project) {
        val java = project.extensions.getByType(JavaPluginExtension::class.java)
        java.sourceCompatibility = JavaVersion.VERSION_25
        java.targetCompatibility = JavaVersion.VERSION_25
        java.withSourcesJar()
        java.withJavadocJar()
    }

    fun configureKotlin(project: Project) {
        val kotlinExt = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        kotlinExt.jvmToolchain(25)
    }

    fun configureRepositories(project: Project) {
        project.repositories.mavenLocal()
        project.repositories.mavenCentral()
        project.repositories.gradlePluginPortal()
        // CNV-17 — JitPack is the single publication channel: project-level
        // repositories override `dependencyResolutionManagement` (Gradle
        // PREFER_PROJECT), so JitPack must be declared here for every consumer.
        project.repositories.maven {
            it.name = "JitPack"
            it.setUrl("https://jitpack.io")
        }
    }

    /**
     * Enables the Gradle build cache for consumers.
     *
     * The code review S-019 flagged this mutation as hostile to the
     * configuration cache (P3-1). It was **verified empirically** against the
     * real plugin on Gradle 9.8.0 / JDK 25: the configuration cache is still
     * stored on the first run and reused on the second (`Configuration cache
     * entry stored` → `Reusing configuration cache`), so the behavior is kept.
     * A `settings.buildCache` rewrite is not available through a supported
     * public API from a `Project`-scoped plugin (only the deprecated
     * `GradleInternal.settings` reaches it), so mutating `startParameter`
     * remains the pragmatic option; revisit when Gradle exposes it publicly.
     */
    fun configureBuildCache(project: Project) {
        project.gradle.startParameter.isBuildCacheEnabled = true
    }

    /**
     * @param options a **lazy** snapshot supplier, evaluated when each `Test`
     *   task is realised. The extension DSL block runs after `apply()`, so an
     *   eager read would ignore every override.
     */
    fun configureTestTasks(project: Project, options: () -> TestTaskOptions) {
        project.tasks.withType(Test::class.java).configureEach(
            Action { test ->
                val effective = options()
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
                if (effective.enableDynamicAgentLoading) {
                    test.jvmArgs("-XX:+EnableDynamicAgentLoading")
                }
                effective.maxHeapSize?.let { heap ->
                    test.maxHeapSize = heap
                }
                if (effective.parallelExecution) {
                    test.systemProperty("junit.jupiter.execution.parallel.enabled", "true")
                }
            }
        )
    }

    /** CNV-7.2 — centralises the JUnit test deps + platform BOM (kills the 6.0.3 bug). */
    fun configureTestDependencies(project: Project) {
        project.afterEvaluate {
            TestDependencies.addPlatformBom(project, "testImplementation")
            TestDependencies.addPlatformBom(project, "testRuntimeOnly")
            TestDependencies.addJunitDeps(project, "testImplementation", "testRuntimeOnly")
        }
    }

    fun registerGuards(project: Project) {
        GradleWrapperVerification.register(project)
        AdoptionAuditTask.register(project)
    }
}
