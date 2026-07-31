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
            project.afterEvaluate {
                project.tasks.findByName("check")?.dependsOn(project.tasks.named("detekt"))
            }
        }
    }
}
