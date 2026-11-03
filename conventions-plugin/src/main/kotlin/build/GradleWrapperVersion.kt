package build

import java.util.Properties

/**
 * Single source of truth for the workspace Gradle wrapper version.
 *
 * The version is **not hardcoded**: it is generated from the plugin's own
 * `gradle/libs.versions.toml` (`[versions].gradle`) into a resource
 * (`build/gradle-version.properties`) and read at runtime, so the guard used
 * by every consumer can never drift from the version this plugin is built
 * with (same anti-drift pattern as [WorkspaceBom]).
 *
 * Pure object (no Gradle type) so the wrapper parsing policy is unit-testable.
 */
object GradleWrapperVersion {

    const val RESOURCE_PATH = "build/gradle-version.properties"

    /** Path of the wrapper file, relative to the consumer project directory. */
    const val WRAPPER_PATH = "gradle/wrapper/gradle-wrapper.properties"

    /** Last-resort fallback if the generated resource is missing. */
    const val FALLBACK_VERSION = "9.8.2"

    private val DISTRIBUTION_PATTERN =
        Regex("""gradle-([0-9]+\.[0-9]+(?:\.[0-9]+)?)-(?:bin|all)\.zip""")

    /** Reads the generated version, falling back to [FALLBACK_VERSION]. */
    fun version(loader: ClassLoader): String {
        val stream = loader.getResourceAsStream(RESOURCE_PATH) ?: return FALLBACK_VERSION
        val properties = stream.use { input -> Properties().also { it.load(input) } }
        val value = properties.getProperty("version")
        return if (value.isNullOrBlank()) FALLBACK_VERSION else value.trim()
    }

    /**
     * Extracts the Gradle distribution version from the content of a
     * `gradle-wrapper.properties` file, or `null` when no distribution URL is
     * present.
     */
    fun distributionVersion(wrapperProperties: String): String? =
        DISTRIBUTION_PATTERN.find(wrapperProperties)?.groupValues?.get(1)

    /** True when the wrapper declares exactly [expected]. */
    fun isAligned(wrapperProperties: String, expected: String): Boolean =
        distributionVersion(wrapperProperties) == expected

    /** Human-readable diagnostic for a drift, or `null` when aligned. */
    fun drift(wrapperProperties: String, expected: String): String? {
        val actual = distributionVersion(wrapperProperties)
            ?: return "no `distributionUrl` found in $WRAPPER_PATH (expected Gradle $expected)"
        if (actual == expected) return null
        return "$WRAPPER_PATH declares Gradle $actual but the workspace requires $expected " +
            "(single source of truth: conventions-plugin gradle/libs.versions.toml [versions].gradle)"
    }
}
