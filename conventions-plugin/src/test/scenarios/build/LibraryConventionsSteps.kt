package build

import io.cucumber.java8.En
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

/**
 * BDD steps for the `library` convention (CNV-14.1). Step texts are prefixed
 * with "library project" to avoid collisions with [GradlePluginConventionsSteps]
 * in the shared `build` glue package (lesson S-088).
 */
class LibraryConventionsSteps : En {

    private lateinit var testProjectDir: File
    private lateinit var taskListResult: BuildResult
    private var depsResult: BuildResult? = null
    private var probeResult: BuildResult? = null

    init {
        Given("a library project applies the library plugin") {
            writeProject()
            taskListResult = runTasks("tasks", "--all")
        }

        Given("a library project applies the library plugin with dependency inspection") {
            writeProject()
            depsResult = runTasks("dependencies", "--configuration", "testImplementation")
            taskListResult = depsResult!!
        }

        Given("a library project applies the library plugin with maxHeapSize {string}") { heap: String ->
            writeProject("maxHeapSize = \"$heap\"")
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the library project has the java-library plugin applied") {
            assert(taskListResult.output.contains("compileJava")) {
                "Expected compileJava (java-library)\n${taskListResult.output}"
            }
        }

        Then("the library project has the kotlin-jvm plugin applied") {
            assert(taskListResult.output.contains("compileKotlin")) {
                "Expected compileKotlin (kotlin-jvm)\n${taskListResult.output}"
            }
        }

        Then("the library project uses Java {int} source compatibility") { version: Int ->
            assertProbe("PROBE_LIB_JAVA_SOURCE=$version")
        }

        Then("the library project uses Java {int} target compatibility") { version: Int ->
            assertProbe("PROBE_LIB_JAVA_TARGET=$version")
        }

        Then("the library project has sources jar task") {
            assert(taskListResult.output.contains("sourcesJar")) {
                "Expected sourcesJar\n${taskListResult.output}"
            }
        }

        Then("the library project has javadoc jar task") {
            assert(taskListResult.output.contains("javadocJar")) {
                "Expected javadocJar\n${taskListResult.output}"
            }
        }

        Then("the library project test dependencies contain the workspace-bom") {
            val expected = WorkspaceBom.coordinates(WorkspaceBom::class.java.classLoader)
            assert(depsResult?.output?.contains(expected) == true) {
                "Expected '$expected' in testImplementation\n${depsResult?.output}"
            }
        }

        Then("the library project test dependencies contain junit-jupiter") {
            assert(depsResult?.output?.contains("org.junit.jupiter:junit-jupiter") == true) {
                "Expected junit-jupiter in testImplementation\n${depsResult?.output}"
            }
        }

        Then("the library project registers the wrapper guard") {
            assert(taskListResult.output.contains("verifyGradleWrapper")) {
                "Expected verifyGradleWrapper\n${taskListResult.output}"
            }
        }

        Then("the library project registers the adoption audit") {
            assert(taskListResult.output.contains("auditConventionsAdoption")) {
                "Expected auditConventionsAdoption\n${taskListResult.output}"
            }
        }

        Then("the library project does not apply the gradle plugin convention") {
            assert(!taskListResult.output.contains("validatePlugins")) {
                "library must not apply java-gradle-plugin (validatePlugins found)\n${taskListResult.output}"
            }
        }

        Then("the library project test tasks use maxHeapSize {string}") { expected: String ->
            assertProbe("PROBE_LIB_TEST_MAXHEAP=$expected")
        }
    }

    private fun writeProject(extensionBody: String = "") {
        testProjectDir = createTempDir("library-conventions-test-")
        testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"library-test-project\"")
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            import org.gradle.api.tasks.testing.Test

            plugins {
                id("education.cccp.build.library")
            }

            libraryConventions {
                $extensionBody
            }

            tasks.register("probeLibrary") {
                doLast {
                    val java = project.extensions.getByName("java") as org.gradle.api.plugins.JavaPluginExtension
                    println("PROBE_LIB_JAVA_SOURCE=" + java.sourceCompatibility.majorVersion)
                    println("PROBE_LIB_JAVA_TARGET=" + java.targetCompatibility.majorVersion)
                    val test = project.tasks.withType(Test::class.java).first()
                    println("PROBE_LIB_TEST_MAXHEAP=" + test.maxHeapSize)
                }
            }
            """.trimIndent()
        )
    }

    private fun runTasks(vararg args: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(*args)
            .withPluginClasspath()
            .build()

    private fun assertProbe(expected: String) {
        val output = probeOutput()
        assert(output.contains(expected)) {
            "Expected '$expected'\n$output"
        }
    }

    private fun probeOutput(): String {
        probeResult?.let { return it.output }
        probeResult = runTasks("probeLibrary")
        return probeResult!!.output
    }

    private fun createTempDir(prefix: String): File {
        val dir = File.createTempFile(prefix, "")
        dir.delete()
        dir.mkdir()
        dir.deleteOnExit()
        return dir
    }
}
