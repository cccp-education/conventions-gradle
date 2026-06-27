package build

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import java.time.Duration

open class CucumberConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "cucumberConventions",
            CucumberConventionsExtension::class.java
        )

        project.afterEvaluate {
            configureCucumber(project, extension)
        }
    }

    private fun configureCucumber(project: Project, extension: CucumberConventionsExtension) {
        project.pluginManager.apply("java")

        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val testSourceSet = sourceSets.getByName("test")
        val mainSourceSet = sourceSets.getByName("main")

        testSourceSet.resources.srcDir(extension.featuresDir)
        testSourceSet.java.srcDir(extension.scenariosDir)

        val cucumberTest = project.tasks.register(extension.cucumberTestTaskName, Test::class.java) { task ->
            task.testClassesDirs = testSourceSet.output.classesDirs
            task.classpath = project.configurations.getByName(testSourceSet.runtimeClasspathConfigurationName) +
                testSourceSet.output +
                mainSourceSet.output +
                project.files(project.tasks.named("jar", Jar::class.java).get().archiveFile)

            configureJUnitPlatform(task, extension.parallel, extension.timeoutMinutes)
        }

        project.tasks.named("test", Test::class.java) { task ->
            task.filter.excludeTestsMatching("*.scenarios.*")
        }

        project.tasks.named("check") { checkTask ->
            checkTask.dependsOn(cucumberTest)
        }

        extension.additionalTasks.forEach { spec ->
            project.tasks.register(spec.name, Test::class.java) { task ->
                task.testClassesDirs = testSourceSet.output.classesDirs
                task.classpath = project.configurations.getByName(testSourceSet.runtimeClasspathConfigurationName) +
                    testSourceSet.output +
                    mainSourceSet.output +
                    project.files(project.tasks.named("jar", Jar::class.java).get().archiveFile)

                if (spec.runnerClass != null) {
                    task.filter.includeTestsMatching(spec.runnerClass)
                }
                if (spec.features.isNotEmpty()) {
                    task.systemProperty("cucumber.features", spec.features.joinToString(","))
                }
                if (spec.tags.isNotEmpty()) {
                    task.systemProperty("cucumber.filter.tags", spec.tags.joinToString(" and "))
                }

                val isParallel = spec.parallel || extension.parallel
                val timeout = spec.timeoutMinutes ?: extension.timeoutMinutes
                configureJUnitPlatform(task, isParallel, timeout)
            }
        }
    }

    private fun configureJUnitPlatform(
        task: Test,
        parallel: Boolean,
        timeoutMinutes: Int?
    ) {
        task.useJUnitPlatform(object : Action<JUnitPlatformOptions> {
            override fun execute(options: JUnitPlatformOptions) {
                options.excludeEngines("junit-jupiter")
            }
        })
        task.systemProperty("cucumber.junit-platform.naming-strategy", "long")

        if (parallel) {
            task.systemProperty("cucumber.execution.parallel.enabled", "true")
        }

        timeoutMinutes?.let { minutes ->
            task.timeout.set(Duration.ofMinutes(minutes.toLong()))
        }
    }
}
