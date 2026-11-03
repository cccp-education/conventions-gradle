package build

import io.cucumber.java8.En
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class GradleWrapperVerificationSteps : En {
    private lateinit var testProjectDir: File
    private lateinit var buildResult: BuildResult

    init {
        Given("a project applies the conventions plugin with wrapper version {string}") { version: String ->
            testProjectDir = createTempDir("gradle-wrapper-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.gradle-plugin")
                }
            """)
            val wrapper = testProjectDir.resolve("gradle/wrapper/gradle-wrapper.properties")
            wrapper.parentFile.mkdirs()
            wrapper.writeText(
                """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\://services.gradle.org/distributions/gradle-$version-bin.zip
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
                """.trimIndent()
            )
        }

        When("I run the wrapper verification") {
            buildResult = runner().build()
        }

        When("I run the wrapper verification expecting failure") {
            buildResult = runner().buildAndFail()
        }

        Then("the wrapper verification succeeds") {
            assert(buildResult.output.contains(":${GradleWrapperVerification.TASK_NAME}")) {
                "Expected the verifyGradleWrapper task to run\n${buildResult.output}"
            }
        }

        Then("the wrapper verification fails naming {string}") { drift: String ->
            assert(buildResult.output.contains(drift)) {
                "Expected the failure to name $drift\n${buildResult.output}"
            }
        }
    }

    private fun runner(): GradleRunner =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(GradleWrapperVerification.TASK_NAME)
            .withPluginClasspath()

    private fun createTempDir(prefix: String): File {
        val dir = File.createTempFile(prefix, "")
        dir.delete()
        dir.mkdir()
        dir.deleteOnExit()
        return dir
    }
}
