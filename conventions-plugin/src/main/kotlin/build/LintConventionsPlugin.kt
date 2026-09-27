package build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class LintConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("org.jlleitschuh.gradle.ktlint")

        val ktlint = project.extensions.getByType(KtlintExtension::class.java)
        ktlint.version.set("1.5.0")

        val javaVersion = System.getProperty("java.version")
        if (javaVersion != null && javaVersion.startsWith("25")) {
            project.logger.warn("detekt 1.23.8 is incompatible with Java 25 — skipped. Upgrade to detekt 2.0+ when available.")
        } else {
            project.pluginManager.apply("io.gitlab.arturbosch.detekt")
            val detekt = project.extensions.getByType(io.gitlab.arturbosch.detekt.extensions.DetektExtension::class.java)
            val configFile = project.rootProject.file("config/detekt/detekt.yml")
            if (configFile.exists()) {
                detekt.config.from(configFile)
            }
            // Robust wiring: react to the `check` task whenever it is registered
            // (base/java plugins may apply after this plugin), instead of the
            // former `findByName("check")?.dependsOn(...)` in afterEvaluate which
            // silently did nothing when `check` was absent at that moment.
            project.tasks.matching { task -> task.name == "check" }.configureEach { checkTask ->
                checkTask.dependsOn("detekt")
            }
        }
    }
}
