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

    private fun configureKover(project: Project) {
        val kover = project.extensions.getByName("kover")
        val koverClass = kover.javaClass

        val currentProject = invokeMethod(kover, koverClass, "currentProject")
        if (currentProject != null) {
            val sources = invokeMethod(currentProject, currentProject.javaClass, "sources")
            if (sources != null) {
                val includedSourceSets = invokeMethod(sources, sources.javaClass, "includedSourceSets")
                if (includedSourceSets != null) {
                    invokeMethod(includedSourceSets, includedSourceSets.javaClass, "addAll", "main", "functionalTest")
                }
            }

            val reports = invokeMethod(currentProject, currentProject.javaClass, "reports")
            if (reports != null) {
                val total = invokeMethod(reports, reports.javaClass, "total")
                if (total != null) {
                    val html = invokeMethod(total, total.javaClass, "html")
                    if (html != null) {
                        val onCheck = invokeMethod(html, html.javaClass, "onCheck")
                        if (onCheck != null) {
                            invokeMethod(onCheck, onCheck.javaClass, "set", true)
                        }
                    }
                    val xml = invokeMethod(total, total.javaClass, "xml")
                    if (xml != null) {
                        val onCheck = invokeMethod(xml, xml.javaClass, "onCheck")
                        if (onCheck != null) {
                            invokeMethod(onCheck, onCheck.javaClass, "set", true)
                        }
                    }
                }
            }
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

    private fun invokeMethod(target: Any, clazz: Class<*>, methodName: String, vararg args: Any?): Any? {
        return try {
            val method = clazz.methods.find { m ->
                m.name == methodName && m.parameterCount == args.size
            }
            method?.invoke(target, *args)
        } catch (_: Exception) {
            null
        }
    }
}
