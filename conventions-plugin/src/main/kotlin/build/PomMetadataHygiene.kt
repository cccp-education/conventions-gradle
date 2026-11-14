package build

/**
 * Sonatype 130791 — a POM `url`/`scm` must point to the **exact public
 * repository**. The bare organization root (`https://github.com/cccp-education`)
 * is not a Git repository, and the `cheroliv` personal path does not exist:
 * Maven Central classifies such components as "commercial" (`unreachable_repo`).
 */
object PomMetadataHygiene {
    private const val ORGANIZATION_ROOT = "https://github.com/cccp-education"

    fun isInvalid(url: String?): Boolean {
        val value = url?.trim()?.removeSuffix("/") ?: return false
        return value == ORGANIZATION_ROOT || value.contains("/cheroliv/")
    }
}
