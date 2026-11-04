package build

/**
 * Immutable snapshot of the JVM test options carried by the convention
 * extensions.
 *
 * A snapshot is taken lazily (when a `Test` task is realised), never at plugin
 * application time: the consumer's `gradlePluginConventions { … }` /
 * `libraryConventions { … }` DSL block is evaluated *after* the `plugins`
 * block, so reading the extension eagerly would silently ignore every
 * override (regression guarded by the existing BDD scenarios).
 */
data class TestTaskOptions(
    val enableDynamicAgentLoading: Boolean,
    val maxHeapSize: String?,
    val parallelExecution: Boolean,
)
