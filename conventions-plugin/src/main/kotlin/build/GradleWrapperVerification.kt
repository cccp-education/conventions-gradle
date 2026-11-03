package build

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

/**
 * Registers the `verifyGradleWrapper` task and wires it to `check`.
 *
 * The task compares the consumer's `gradle/wrapper/gradle-wrapper.properties`
 * against the workspace version carried by the plugin (single source of truth:
 * `gradle/libs.versions.toml [versions].gradle`). Because it is wired to
 * `check`, every consumer's GitHub Actions job enforces it for free — this is
 * the maintenance system for the wrapper drift.
 *
 * Registered on the root project only: a multi-project build declares its
 * wrapper once at the root.
 */
internal object GradleWrapperVerification {

    const val TASK_NAME = "verifyGradleWrapper"

    fun register(project: Project) {
        if (project.rootProject != project) return
        if (project.tasks.names.contains(TASK_NAME)) return

        val expected = GradleWrapperVersion.version(GradleWrapperVersion::class.java.classLoader)
        val wrapperFile = project.layout.projectDirectory.file(GradleWrapperVersion.WRAPPER_PATH)

        val verify: TaskProvider<*> = project.tasks.register(TASK_NAME) { task ->
            task.group = "verification"
            task.description =
                "Verifies that the Gradle wrapper matches the workspace version ($expected)"
            task.inputs.property("expectedGradleVersion", expected)
            task.doLast {
                val file = wrapperFile.asFile
                // A project without a wrapper is not a wrapper consumer: keep
                // `check` green (TestKit fixture projects, includeBuild
                // modules). Only an existing, drifting wrapper fails.
                if (!file.exists()) {
                    task.logger.warn(
                        "No Gradle wrapper at ${file.path} — version guard skipped (workspace requires $expected)"
                    )
                    return@doLast
                }
                val drift = GradleWrapperVersion.drift(file.readText(), expected)
                if (drift != null) throw GradleException(drift)
            }
        }

        project.tasks.matching { it.name == "check" }.configureEach { it.dependsOn(verify) }
    }
}
