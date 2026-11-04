package build

import javax.inject.Inject

/**
 * Options of the [LibraryConventionsPlugin].
 *
 * Mirrors the JVM options of `gradlePluginConventions` (a library and a plugin
 * project share the same test policy). `fixAnnotationsConflict` is intentionally
 * absent: the buildscript annotations workaround is a plugin-project concern,
 * not a contract/library concern.
 */
open class LibraryConventionsExtension @Inject constructor() {
    var enableDynamicAgentLoading: Boolean = true
    var maxHeapSize: String? = null
    var parallelExecution: Boolean = false
}
