package build

import java.util.Properties

/**
 * Pure domain concept describing the workspace-bom platform injected on
 * consumer test classpaths.
 *
 * The version is **not hardcoded**: it is generated from the plugin's own
 * `gradle/libs.versions.toml` (`[versions].workspace-bom`) into a resource
 * (`build/workspace-bom.properties`) and read at runtime. This kills the drift
 * found in S-019 (the constant had stayed at `0.0.13` while the published BOM
 * had reached `0.0.58`, 45 versions behind, silently pinning an obsolete
 * platform for every consumer without a local `libs` catalog).
 *
 * No Gradle type is involved, so the resolution policy is unit-testable.
 */
object WorkspaceBom {

    const val GROUP = "education.cccp"
    const val ARTIFACT = "workspace-bom"
    const val RESOURCE_PATH = "build/workspace-bom.properties"

    /** Last-resort fallback if the generated resource is missing. */
    const val FALLBACK_VERSION = "0.0.58"

    /** The Maven coordinates `group:artifact:version` for a given [version]. */
    fun coordinates(version: String): String = "$GROUP:$ARTIFACT:$version"

    /**
     * Reads the generated version from [RESOURCE_PATH] on the given [loader]
     * classpath. Falls back to [FALLBACK_VERSION] when the resource is absent,
     * unreadable, or carries a blank version — never returns an empty version.
     */
    fun version(loader: ClassLoader): String {
        val stream = loader.getResourceAsStream(RESOURCE_PATH) ?: return FALLBACK_VERSION
        val properties = stream.use { input ->
            Properties().also { it.load(input) }
        }
        val value = properties.getProperty("version")
        return if (value.isNullOrBlank()) FALLBACK_VERSION else value
    }

    /** The Maven coordinates resolved from the plugin's own classpath. */
    fun coordinates(loader: ClassLoader): String = coordinates(version(loader))
}
