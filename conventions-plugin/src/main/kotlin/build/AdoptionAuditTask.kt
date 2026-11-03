package build

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

/**
 * Registers `auditConventionsAdoption`, the cross-borough guard for CNV-13.5.
 *
 * The task scans every `build.gradle.kts` under `-PworkspaceRoot=<path>` and
 * reports pins that are ahead of, behind, or invalid relative to the published
 * conventions version (the plugin's own version). It is **on demand** (not
 * wired to `check`): a CI job has no access to the workspace, so it is invoked
 * by the release session or by a cross-borough workflow that checks out the
 * workspace. `-Pstrict=true` turns findings into a build failure.
 */
internal object AdoptionAuditTask {

    const val TASK_NAME = "auditConventionsAdoption"

    private val IGNORED_DIRECTORIES = setOf("build", ".gradle", ".git", "node_modules", ".kotlin")

    fun register(project: Project) {
        if (project.rootProject != project) return
        if (project.tasks.names.contains(TASK_NAME)) return

        val expected = ConventionsVersion.version(ConventionsVersion::class.java.classLoader)

        project.tasks.register(TASK_NAME) { task ->
            task.group = "verification"
            task.description =
                "Audits education.cccp.build pins under -PworkspaceRoot against $expected"
            task.outputs.upToDateWhen { false }

            task.doLast {
                val rootPath = project.findProperty("workspaceRoot")?.toString()
                    ?: throw GradleException(
                        "Pass -PworkspaceRoot=<workspace path> to audit the conventions adoption"
                    )
                val root = File(rootPath)
                if (!root.isDirectory) {
                    throw GradleException("workspaceRoot '$rootPath' is not a directory")
                }

                val files = root.walkTopDown()
                    .onEnter { it.name !in IGNORED_DIRECTORIES }
                    .onFail { _, _ -> }
                    .filter { it.isFile && it.name == "build.gradle.kts" }
                    .associate { it.relativeTo(root).path to it.readText() }

                val findings = AdoptionAudit.audit(files, expected)
                if (findings.isEmpty()) {
                    println("CONVENTIONS ADOPTION OK: ${files.size} build file(s) at $expected")
                    return@doLast
                }

                println("CONVENTIONS ADOPTION: ${findings.size} finding(s) against $expected")
                findings.forEach { println("  - ${AdoptionAudit.describe(it)}") }

                if (project.findProperty("strict")?.toString() == "true") {
                    throw GradleException("${findings.size} conventions adoption finding(s)")
                }
            }
        }
    }
}
