package build

import io.cucumber.java8.En
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class LogbackExclusionConventionsSteps : En {

    private lateinit var testProjectDir: File
    private lateinit var depsResult: BuildResult

    init {
        Given("a project applying the logback exclusion convention") {
            testProjectDir = createTempDir("logback-exclusion-test-")
            testProjectDir.resolve("settings.gradle.kts")
                .writeText("rootProject.name = \"logback-exclusion-test\"")
            testProjectDir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    java
                    id("education.cccp.build.functional-test")
                    id("education.cccp.build.logback-exclusion")
                }

                dependencies {
                    testImplementation("org.slf4j:slf4j-api:2.0.17")
                    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.26")
                }
                """.trimIndent()
            )
        }

        Then("the {string} configuration excludes logback-classic") { configName: String ->
            depsResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("dependencies", "--configuration", configName)
                .withPluginClasspath()
                .build()
            assert(!depsResult.output.contains("ch.qos.logback:logback-classic")) {
                "Expected logback-classic excluded from $configName\n${depsResult.output}"
            }
        }

        Then("the {string} configuration excludes logback-classic without excluding slf4j") { configName: String ->
            depsResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("dependencies", "--configuration", configName)
                .withPluginClasspath()
                .build()
            assert(!depsResult.output.contains("ch.qos.logback:logback-classic")) {
                "Expected logback-classic excluded from $configName\n${depsResult.output}"
            }
            assert(depsResult.output.contains("org.slf4j:slf4j-api")) {
                "Expected slf4j-api preserved in $configName\n${depsResult.output}"
            }
        }

        Then("the {string} configuration does not exclude slf4j-api") { configName: String ->
            depsResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("dependencies", "--configuration", configName)
                .withPluginClasspath()
                .build()
            assert(depsResult.output.contains("org.slf4j:slf4j-api")) {
                "Expected slf4j-api preserved in $configName\n${depsResult.output}"
            }
        }
    }

    private fun createTempDir(prefix: String): File {
        val dir = File.createTempFile(prefix, "")
        dir.delete()
        dir.mkdir()
        dir.deleteOnExit()
        return dir
    }
}