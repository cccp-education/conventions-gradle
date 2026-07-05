package build

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension

/**
 * Socle partagé de gestion des dépendances de test pour les convention plugins.
 *
 * Single responsibility (DDD) : centraliser l'ajout de deps test (junit, cucumber,
 * platform BOM) avec fallback hardcoded quand le catalogue local `libs` est absent
 * du projet consommateur. Élimine la duplication des versions divergentes
 * (bug `junit-platform-params = "6.0.3"` dans plantuml-gradle).
 *
 * Cascade de résolution : catalogue `libs` du projet consommateur > fallback hardcoded.
 */
object TestDependencies {

    const val WORKSPACE_BOM_COORDINATES = "education.cccp:workspace-bom:0.0.4"

    val CUCUMBER_FALLBACKS: Map<String, String> = mapOf(
        "cucumber-java" to "io.cucumber:cucumber-java:7.34.3",
        "cucumber-junit-platform-engine" to "io.cucumber:cucumber-junit-platform-engine:7.34.3",
        "cucumber-picocontainer" to "io.cucumber:cucumber-picocontainer:7.34.3",
        "cucumber-java8" to "io.cucumber:cucumber-java8:7.34.3",
        "junit-platform-suite" to "org.junit.platform:junit-platform-suite:1.14.3"
    )

    val JUNIT_FALLBACKS: Map<String, String> = mapOf(
        "kotlin-test-junit5" to "org.jetbrains.kotlin:kotlin-test-junit5:2.3.20",
        "junit-jupiter" to "org.junit.jupiter:junit-jupiter:5.12.2",
        "junit-platform-launcher" to "org.junit.platform:junit-platform-launcher:1.14.3",
        "junit-platform-params" to "org.junit.jupiter:junit-jupiter-params:5.12.2",
        "assertj-core" to "org.assertj:assertj-core:3.27.7"
    )

    fun libs(project: Project): VersionCatalog? = try {
        project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    } catch (_: Exception) {
        null
    }

    fun addPlatformBom(project: Project, config: String) {
        try {
            project.dependencies.add(config, project.dependencies.platform(WORKSPACE_BOM_COORDINATES))
        } catch (_: Exception) {
        }
    }

    fun addFromCatalog(
        project: Project,
        libs: VersionCatalog?,
        config: String,
        catalogKey: String,
        fallback: String
    ) {
        if (libs != null) {
            val provider = libs.findLibrary(catalogKey)
            if (provider.isPresent) {
                project.dependencies.add(config, provider.get())
                return
            }
        }
        project.dependencies.add(config, fallback)
    }

    fun addCucumberDeps(project: Project, config: String) {
        val libs = libs(project)
        CUCUMBER_FALLBACKS.forEach { (key, fallback) ->
            addFromCatalog(project, libs, config, key, fallback)
        }
    }

    fun addJunitDeps(project: Project, implementationConfig: String, runtimeOnlyConfig: String) {
        val libs = libs(project)
        JUNIT_FALLBACKS.forEach { (key, fallback) ->
            when (key) {
                "junit-platform-launcher" -> addFromCatalog(project, libs, runtimeOnlyConfig, key, fallback)
                else -> addFromCatalog(project, libs, implementationConfig, key, fallback)
            }
        }
    }
}