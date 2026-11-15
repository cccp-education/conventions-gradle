package build

import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * GBL-014 — registers `verifyChannel`, the static consistency gate between the
 * selected [PublicationChannel] and the build's `settings.gradle(.kts)`.
 *
 * The task is registered unconditionally (it is useful on demand) but wired to
 * `check` **only when `-Pchannel` is explicitly provided**. That is the
 * transition rule: the ~20 public consumers that have not migrated yet keep
 * their current CI unchanged; the rollout (GBL-014-5) makes `-Pchannel`
 * explicit in `ci/run.sh`, which activates the gate.
 *
 * GBL-014-4 / S-052 — registration is shared by **every** convention plugin in
 * this module ([GradlePluginConventionsPlugin], [LibraryConventionsPlugin],
 * [CucumberConventionsPlugin], [FunctionalTestConventionsPlugin],
 * [KoverConventionsPlugin], [LintConventionsPlugin],
 * [LogbackExclusionConventionsPlugin], [PublishingConventionsPlugin]): a build
 * that only adopts `.cucumber` or `.library` used to have no gate at all. The
 * task is registered unconditionally (it is useful on demand) and wired to
 * `check` only when `-Pchannel` is explicit. Registration is idempotent, so a
 * build applying several conventions still gets exactly one `verifyChannel`.
 */
internal object ChannelVerificationTask {

    const val TASK_NAME = "verifyChannel"

    fun register(project: Project) {
        if (project.tasks.names.contains(TASK_NAME)) return

        project.tasks.register(TASK_NAME) { task ->
            task.group = "verification"
            task.description =
                "Audits settings.gradle(.kts) consistency with -P${PublicationChannel.PROPERTY} (GBL-014)."
            task.outputs.upToDateWhen { false }

            task.doLast {
                val channel = PublicationChannel.parse(
                    project.findProperty(PublicationChannel.PROPERTY)?.toString(),
                )
                val settings = listOf("settings.gradle.kts", "settings.gradle")
                    .map { project.rootProject.file(it) }
                    .firstOrNull { it.isFile }

                if (settings == null) {
                    println("CHANNEL OK: no settings file to audit (channel=${channel.id})")
                    return@doLast
                }

                val findings = ChannelConsistency.auditSettings(settings.readText(), channel)
                if (findings.isEmpty()) {
                    println("CHANNEL OK: ${settings.name} is consistent with channel=${channel.id}")
                    return@doLast
                }

                findings.forEach { println("  - ${ChannelConsistency.describe(it)}") }
                throw GradleException(
                    "${findings.size} channel consistency finding(s) in ${settings.name} " +
                        "for channel=${channel.id}",
                )
            }
        }

        val explicitChannel = project.findProperty(PublicationChannel.PROPERTY) != null
        if (explicitChannel) {
            project.tasks.matching { it.name == "check" }.configureEach { task ->
                task.dependsOn(TASK_NAME)
            }
        }
    }
}
