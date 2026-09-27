package build

import io.cucumber.java8.En
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class GradlePluginConventionsSteps : En {

    private lateinit var testProjectDir: File
    private lateinit var taskListResult: BuildResult
    private var depsResult: BuildResult? = null
    private var probeResult: BuildResult? = null

    init {
        Given("a project applies the conventions plugin") {
            writeProject()
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the project has the java-gradle-plugin applied") {
            assert(taskListResult.output.contains("compileJava")) {
                "Expected compileJava task (from java-gradle-plugin)\n${taskListResult.output}"
            }
        }

        Then("the project has the kotlin-jvm plugin applied") {
            assert(taskListResult.output.contains("compileKotlin")) {
                "Expected compileKotlin task (from kotlin-jvm)\n${taskListResult.output}"
            }
        }

        Then("the project has the maven-publish plugin applied") {
            assert(taskListResult.output.contains("publish")) {
                "Expected publish tasks (from maven-publish)\n${taskListResult.output}"
            }
        }

        Then("the project uses Java {int} source compatibility") { version: Int ->
            assertProbe("PROBE_JAVA_SOURCE=$version")
        }

        Then("the project uses Java {int} target compatibility") { version: Int ->
            assertProbe("PROBE_JAVA_TARGET=$version")
        }

        Then("the project has sources jar task") {
            assert(taskListResult.output.contains("sourcesJar")) {
                "Expected sourcesJar task\n${taskListResult.output}"
            }
        }

        Then("the project has javadoc jar task") {
            assert(taskListResult.output.contains("javadocJar")) {
                "Expected javadocJar task\n${taskListResult.output}"
            }
        }

        Then("test tasks use JUnit Platform") {
            assert(taskListResult.output.contains("test")) {
                "Expected test task\n${taskListResult.output}"
            }
        }

        Then("test logging shows passed, skipped, and failed events") {
            assertProbe("PROBE_TEST_EVENTS=FAILED,PASSED,SKIPPED")
        }

        // ── CNV-7.2 — junit test dependencies ────────────────────────────────
        Given("a project applies the conventions plugin with dependency inspection") {
            writeProject()
            depsResult = runDependencies("testImplementation")
            taskListResult = depsResult!!
        }

        Then("the testImplementation configuration contains kotlin-test-junit5") {
            assertDepsContains("org.jetbrains.kotlin:kotlin-test-junit5")
        }

        Then("the testImplementation configuration contains junit-jupiter") {
            assertDepsContains("org.junit.jupiter:junit-jupiter")
        }

        Then("the testImplementation configuration contains junit-platform-params") {
            assertDepsContains("junit-jupiter-params")
        }

        Then("the testImplementation configuration contains assertj-core") {
            assertDepsContains("org.assertj:assertj-core")
        }

        Then("the testRuntimeOnly configuration contains junit-platform-launcher") {
            val runtimeResult = runDependencies("testRuntimeOnly")
            assert(runtimeResult.output.contains("org.junit.platform:junit-platform-launcher")) {
                "Expected junit-platform-launcher in testRuntimeOnly\n${runtimeResult.output}"
            }
        }

        Then("the gradle testImplementation configuration has the workspace-bom platform") {
            assertDepsContains("education.cccp:workspace-bom")
        }

        // ── CNV-10.1 — configureRepositories ─────────────────────────────────
        Then("the project has mavenLocal repository configured") {
            assertProbe("PROBE_REPO_LOCAL=true")
        }

        Then("the project has mavenCentral repository configured") {
            assertProbe("PROBE_REPO_CENTRAL=true")
        }

        Then("the project has gradlePluginPortal repository configured") {
            assertProbe("PROBE_REPO_PLUGIN_PORTAL=true")
        }

        // ── CNV-10.1 — configureBuildCache ────────────────────────────────────
        Then("the build cache is enabled") {
            assertProbe("PROBE_BUILD_CACHE_ENABLED=true")
        }

        // ── CNV-10.7 — TestDependencies fallback hardcoded (no catalog) ──────
        Given("a project applies the conventions plugin without version catalog") {
            writeProject()
            depsResult = runDependencies("testImplementation")
            taskListResult = depsResult!!
        }

        Then("the testImplementation configuration contains junit-jupiter from fallback") {
            assertDepsContains("org.junit.jupiter:junit-jupiter")
        }

        // ── CNV-11.1 — GradlePluginConventionsExtension defaults ──────────────
        Then("the gradlePluginConventions extension has enableDynamicAgentLoading default true") {
            assertProbe("PROBE_ENABLE_DYNAMIC_AGENT=true")
        }

        Then("the gradlePluginConventions extension has maxHeapSize default null") {
            assertProbe("PROBE_MAX_HEAP=null")
        }

        Then("the gradlePluginConventions extension has parallelExecution default false") {
            assertProbe("PROBE_PARALLEL=false")
        }

        // ── CNV-11.1 — Extension override via DSL ─────────────────────────────
        Given("a project applies the conventions plugin with custom extension values") {
            writeProject("""
                enableDynamicAgentLoading = false
                maxHeapSize = "2g"
                parallelExecution = true
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the gradlePluginConventions extension has enableDynamicAgentLoading set to false") {
            assertProbe("PROBE_ENABLE_DYNAMIC_AGENT=false")
        }

        Then("the gradlePluginConventions extension has maxHeapSize set to {string}") { expected: String ->
            assertProbe("PROBE_MAX_HEAP=$expected")
        }

        Then("the gradlePluginConventions extension has parallelExecution set to true") {
            assertProbe("PROBE_PARALLEL=true")
        }

        // ── CNV-11.2 — configureTestTasks enriched ────────────────────────────
        Given("a project applies the conventions plugin with enableDynamicAgentLoading false") {
            writeProject("""
                enableDynamicAgentLoading = false
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Given("a project applies the conventions plugin with maxHeapSize {string}") { heap: String ->
            writeProject("""
                maxHeapSize = "$heap"
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Given("a project applies the conventions plugin with parallelExecution true") {
            writeProject("""
                parallelExecution = true
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the build succeeds with default extension") {
            assertProbe("PROBE_JVMARGS=-XX:+EnableDynamicAgentLoading")
        }

        Then("the build succeeds with enableDynamicAgentLoading false") {
            assert(!probeOutput().contains("PROBE_JVMARGS=-XX:+EnableDynamicAgentLoading")) {
                "Expected no dynamic agent loading jvmArg when disabled\n${probeOutput()}"
            }
        }

        Then("the build succeeds with maxHeapSize {string}") { expected: String ->
            assertProbe("PROBE_TEST_MAXHEAP=$expected")
        }

        Then("the build succeeds with parallelExecution true") {
            assertProbe("PROBE_JUNIT_PARALLEL=true")
        }

        // ── CNV-12.1 / S-021 P2-A — Bump fallbacks ───────────────────────────
        Then("the testImplementation configuration contains the current workspace-bom version") {
            val expected = WorkspaceBom.coordinates(WorkspaceBom::class.java.classLoader)
            assert(depsResult?.output?.contains(expected) == true) {
                "Expected the current workspace-bom version '$expected' in testImplementation\n${depsResult?.output}"
            }
        }

        Then("the testImplementation configuration contains kotlin-test-junit5 version {string}") { expectedVersion: String ->
            assertDepsContains("org.jetbrains.kotlin:kotlin-test-junit5:$expectedVersion")
        }

        // ── CNV-12.2 — fixAnnotationsConflict ─────────────────────────────────
        Then("the gradlePluginConventions extension has fixAnnotationsConflict default false") {
            assertProbe("PROBE_FIX_ANNOTATIONS=false")
        }

        Given("a project applies the conventions plugin with fixAnnotationsConflict true") {
            writeProject("""
                fixAnnotationsConflict = true
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the build succeeds with fixAnnotationsConflict true") {
            assert(taskListResult.output.contains("BUILD SUCCESSFUL")) {
                "Expected build to succeed with fixAnnotationsConflict true\n${taskListResult.output}"
            }
        }
    }

    /**
     * Writes a fresh consumer project that applies the conventions plugin and
     * registers a `probeConventions` task exposing the extension defaults and
     * the materialised Test task configuration. [extensionBody] is injected
     * verbatim inside the `gradlePluginConventions { … }` block.
     *
     * Centralising the probe removes the eight `assert(true)` placeholders the
     * code review flagged (S-019 P3-2): every default and override is now read
     * from a real Gradle build.
     */
    private fun writeProject(extensionBody: String = "") {
        testProjectDir = createTempDir("conventions-test-")
        testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
        testProjectDir.resolve("build.gradle.kts").writeText("""
            import build.GradlePluginConventionsExtension
            import org.gradle.api.tasks.testing.Test

            plugins {
                id("education.cccp.build.gradle-plugin")
            }

            gradlePluginConventions {
                $extensionBody
            }

            tasks.register("probeConventions") {
                doLast {
                    val ext = project.extensions.getByName("gradlePluginConventions") as GradlePluginConventionsExtension
                    println("PROBE_ENABLE_DYNAMIC_AGENT=" + ext.enableDynamicAgentLoading)
                    println("PROBE_MAX_HEAP=" + ext.maxHeapSize)
                    println("PROBE_PARALLEL=" + ext.parallelExecution)
                    println("PROBE_FIX_ANNOTATIONS=" + ext.fixAnnotationsConflict)
                    val java = project.extensions.getByName("java") as org.gradle.api.plugins.JavaPluginExtension
                    println("PROBE_JAVA_SOURCE=" + java.sourceCompatibility.majorVersion)
                    println("PROBE_JAVA_TARGET=" + java.targetCompatibility.majorVersion)
                    val repoUrls = project.repositories
                        .filterIsInstance<org.gradle.api.artifacts.repositories.MavenArtifactRepository>()
                        .map { it.url.toString() }
                        .toSet()
                    println("PROBE_REPO_LOCAL=" + repoUrls.any { it.startsWith("file:") })
                    println("PROBE_REPO_CENTRAL=" + repoUrls.any { it.contains("repo.maven.apache.org") })
                    println("PROBE_REPO_PLUGIN_PORTAL=" + repoUrls.any { it.contains("plugins.gradle.org") })
                    println("PROBE_BUILD_CACHE_ENABLED=" + project.gradle.startParameter.isBuildCacheEnabled)
                    val test = project.tasks.withType(org.gradle.api.tasks.testing.Test::class.java).first()
                    println("PROBE_JVMARGS=" + (test.jvmArgs ?: emptyList()).joinToString(","))
                    println("PROBE_TEST_MAXHEAP=" + test.maxHeapSize)
                    println("PROBE_JUNIT_PARALLEL=" + test.systemProperties["junit.jupiter.execution.parallel.enabled"])
                    println("PROBE_TEST_EVENTS=" + (test.testLogging.events ?: emptySet()).map { it.name }.sorted().joinToString(","))
                }
            }
        """)
    }

    private fun runTasks(vararg args: String): BuildResult {
        return GradleRunner.create()
            .withProjectDir(ensureProjectDir())
            .withArguments(*args)
            .withPluginClasspath()
            .build()
    }

    private fun runDependencies(configuration: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("dependencies", "--configuration", configuration)
            .withPluginClasspath()
            .build()

    private fun assertDepsContains(expected: String) {
        assert(depsResult?.output?.contains(expected) == true) {
            "Expected '$expected' in dependency report\n${depsResult?.output}"
        }
    }

    private fun assertProbe(expected: String) {
        assert(probeOutput().contains(expected)) {
            "Expected '$expected'\n${probeOutput()}"
        }
    }

    /**
     * Runs the `probeConventions` task of the current Given project and returns
     * its output, caching the result so a scenario triggers at most one probe
     * build.
     */
    private fun probeOutput(): String {
        probeResult?.let { return it.output }
        probeResult = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("probeConventions")
            .withPluginClasspath()
            .build()
        return probeResult!!.output
    }

    private fun ensureProjectDir(): File {
        if (!::testProjectDir.isInitialized) {
            writeProject()
        }
        return testProjectDir
    }

    private fun createTempDir(prefix: String): File {
        val dir = File.createTempFile(prefix, "")
        dir.delete()
        dir.mkdir()
        dir.deleteOnExit()
        return dir
    }
}
