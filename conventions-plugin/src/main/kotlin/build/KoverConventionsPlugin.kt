package build

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project

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

    private fun configureThresholdCheck(project: Project, extension: KoverConventionsExtension) {
        extension.threshold?.let { thresholdValue ->
            val thresholdTask = project.tasks.register("koverThresholdCheck", DefaultTask::class.java) { task ->
                task.description = "kover threshold check"
                task.dependsOn("koverXmlReport")

                task.doLast {
                    val reportFile = project.layout.buildDirectory
                        .file("reports/kover/xml/report.xml")
                        .get()
                        .asFile
                    if (!reportFile.exists()) {
                        throw RuntimeException("Kover report not found. Run 'koverXmlReport' first.")
                    }
                    val xml = reportFile.readText()
                    val coverageRegex = Regex("""<counter type="INSTRUCTION" missed="(\d+)" covered="(\d+)"/>""")
                    val matches = coverageRegex.findAll(xml)
                    var totalMissed = 0L
                    var totalCovered = 0L
                    for (match in matches) {
                        totalMissed += match.groupValues[1].toLong()
                        totalCovered += match.groupValues[2].toLong()
                    }
                    val total = totalMissed + totalCovered
                    val coverage = if (total > 0) (totalCovered.toDouble() / total) * 100 else 0.0
                    println(
                        "Instruction coverage: ${
                            String.format("%.2f", coverage)
                        }% (missed=$totalMissed, covered=$totalCovered)"
                    )
                    if (coverage < thresholdValue) {
                        throw RuntimeException(
                            "Coverage ${String.format("%.2f", coverage)}% is below threshold ${thresholdValue}%"
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
