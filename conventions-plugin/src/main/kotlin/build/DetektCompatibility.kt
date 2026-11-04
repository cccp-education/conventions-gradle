package build

/**
 * Detekt availability policy (CNV-15.2).
 *
 * detekt 1.23.8 is the latest release and its embedded compiler only accepts a
 * JVM target up to 22. The former guard hardcoded `java.version.startsWith("25")`,
 * which would silently try detekt on JDK 26+ and fail. This pure object makes
 * the policy explicit and unit-testable.
 */
object DetektCompatibility {

    /** Highest Java major version detekt 1.23.8 supports. */
    const val MAX_SUPPORTED_JAVA_VERSION = 22

    /**
     * @return `true` when detekt can run on [javaVersion] (`java.version`
     *   format, e.g. `21.0.1`, `25.0.5`, `1.8.0_402`). An unparsable value is
     *   treated as unsupported (safer: skip detekt instead of failing the build).
     */
    fun supports(javaVersion: String): Boolean {
        val major = javaVersion
            .substringBefore('.')
            .substringBefore('-')
            .toIntOrNull()
            ?: return false
        return major in 1..MAX_SUPPORTED_JAVA_VERSION
    }

    /** Human-readable diagnostic used by the lint plugin warning. */
    fun diagnostic(javaVersion: String): String =
        "detekt 1.23.8 supports JVM <= $MAX_SUPPORTED_JAVA_VERSION (current: $javaVersion) — skipped."
}
