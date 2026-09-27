package build

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

open class KoverConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "koverConventions",
            KoverConventionsExtension::class.java
        )

        project.afterEvaluate {
            if (!extension.enabled) return@afterEvaluate

            project.pluginManager.apply("org.jetbrains.kotlinx.kover")

            configureKover(project)
            configureThresholdCheck(project, extension)
        }
    }

    /**
     * Configures the Kover extension through its typed DSL: only [KoverReportSources]
     * are measured, and the HTML/XML reports are attached to `check`.
     *
     * A former reflective implementation silently did nothing: it looked up a
     * zero-argument `currentProject` method on the extension, but the real member
     * is the `currentProject(Action)` function (the getter is `getCurrentProject`),
     * and `reports` lives on the extension itself, not on the variant config.
     * Replaced by direct typed access so the configuration can no longer be skipped.
     */
    private fun configureKover(project: Project) {
        val kover = project.extensions.getByType(KoverProjectExtension::class.java)

        kover.currentProject { variant ->
            variant.sources { sources -> sources.includedSourceSets.addAll(KoverReportSources.included) }
        }

        kover.reports.total { report ->
            report.html { html -> html.onCheck.set(true) }
            report.xml { xml -> xml.onCheck.set(true) }
        }
    }

    /**
     * Resolves the XML report file kover will write, asking the `kover`
     * extension for the configured `reports.total.xml.xmlFile`. This is the
     * canonical source of truth: it respects a consumer override of `xmlFile`,
     * and it is immune to the `reports/kover/xml/report.xml` vs
     * `reports/kover/report.xml` drift fixed in 0.0.5. Falls back to the
     * kover 0.9.8 default when the property cannot be read.
     */
    private fun resolveXmlReportFile(project: Project): File {
        val kover = runCatching { project.extensions.getByName("kover") }.getOrNull()
        if (kover is KoverProjectExtension) {
            val configured = runCatching { kover.reports.total.xml.xmlFile.orNull }.getOrNull()
            if (configured != null) return configured.asFile
        }
        return project.layout.buildDirectory
            .file("reports/kover/report.xml")
            .get()
            .asFile
    }

    private fun configureThresholdCheck(project: Project, extension: KoverConventionsExtension) {
        extension.threshold?.let { thresholdValue ->
            val thresholdTask = project.tasks.register("koverThresholdCheck", DefaultTask::class.java) { task ->
                task.description = "kover threshold check"
                task.dependsOn("koverXmlReport")

                task.doLast {
                    val reportFile = resolveXmlReportFile(project)
                    if (!reportFile.exists()) {
                        throw RuntimeException("Kover report not found. Run 'koverXmlReport' first.")
                    }
                    val summary = KoverCoverage.summarize(reportFile.readText())
                    println(
                        "Instruction coverage: ${
                            String.format("%.2f", summary.percent)
                        }% (missed=${summary.missed}, covered=${summary.covered})"
                    )
                    if (summary.percent < thresholdValue) {
                        throw RuntimeException(
                            "Coverage ${String.format("%.2f", summary.percent)}% is below threshold ${thresholdValue}%"
                        )
                    }
                }
            }
            project.tasks.named("check") { checkTask ->
                checkTask.dependsOn(thresholdTask)
            }
        }
    }
}
