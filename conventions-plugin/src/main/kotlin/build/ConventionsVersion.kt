package build

import java.util.Properties

/**
 * The version of the conventions plugin itself, materialised as a resource by
 * the build (`generateConventionsVersionInfo`). Used by the adoption audit so
 * the expected version can never drift from the published plugin.
 */
object ConventionsVersion {

    const val RESOURCE_PATH = "build/conventions-version.properties"

    fun version(loader: ClassLoader): String {
        val stream = loader.getResourceAsStream(RESOURCE_PATH)
            ?: return "unknown"
        val properties = stream.use { input -> Properties().also { it.load(input) } }
        val value = properties.getProperty("version")
        return if (value.isNullOrBlank()) "unknown" else value.trim()
    }
}
