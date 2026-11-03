package build

/**
 * Cross-borough adoption audit (CNV-13.5).
 *
 * Scans the `education.cccp.build.*` plugin pins declared by the workspace
 * builds and compares them to the conventions version that is actually
 * published (the plugin's own version). Pure domain: the file contents are
 * injected, so the policy is unit-testable without a Gradle build and without
 * reading another repository's working tree (D5-RACE lesson).
 */
object AdoptionAudit {

    data class Pin(val file: String, val plugin: String, val version: String)

    sealed interface Finding {
        val file: String
        val plugin: String
        val version: String

        /** The consumer pins a version newer than the published conventions. */
        data class Ahead(
            override val file: String,
            override val plugin: String,
            override val version: String,
            val expected: String,
        ) : Finding

        /** The consumer pins an older version than the published conventions. */
        data class Obsolete(
            override val file: String,
            override val plugin: String,
            override val version: String,
            val expected: String,
        ) : Finding

        /** `-SNAPSHOT` pins are never valid in the workspace. */
        data class Snapshot(
            override val file: String,
            override val plugin: String,
            override val version: String,
        ) : Finding

        /** The pinned version is not semver-like (cannot be compared). */
        data class Invalid(
            override val file: String,
            override val plugin: String,
            override val version: String,
        ) : Finding
    }

    // Kotlin DSL: id("education.cccp.build.<plugin>") version "<version>"
    private val KOTLIN_PIN =
        Regex("""education\.cccp\.build\.([a-z-]+)"\)\s*version\s*"([^"]+)"""")
    // Groovy DSL fallback: id 'education.cccp.build.<plugin>' version '<version>'
    private val GROOVY_PIN =
        Regex("""education\.cccp\.build\.([a-z-]+)'\s*version\s*'([^']+)'""")

    /** All pins declared in one build file. */
    fun pins(file: String, content: String): List<Pin> {
        val kotlin = KOTLIN_PIN.findAll(content).map {
            Pin(file, it.groupValues[1], it.groupValues[2])
        }
        val groovy = GROOVY_PIN.findAll(content).map {
            Pin(file, it.groupValues[1], it.groupValues[2])
        }
        return (kotlin + groovy).toList()
    }

    /** Audits every `file -> content` pair against [expected]. */
    fun audit(files: Map<String, String>, expected: String): List<Finding> =
        files.entries
            .flatMap { (file, content) -> pins(file, content) }
            .mapNotNull { pin -> finding(pin, expected) }
            .sortedWith(compareBy({ it.file }, { it.plugin }, { it.version }))

    private fun finding(pin: Pin, expected: String): Finding? {
        if (pin.version.endsWith("-SNAPSHOT")) {
            return Finding.Snapshot(pin.file, pin.plugin, pin.version)
        }
        val comparison = compare(pin.version, expected)
            ?: return Finding.Invalid(pin.file, pin.plugin, pin.version)
        return when {
            comparison > 0 -> Finding.Ahead(pin.file, pin.plugin, pin.version, expected)
            comparison < 0 -> Finding.Obsolete(pin.file, pin.plugin, pin.version, expected)
            else -> null
        }
    }

    /**
     * Numeric semver comparison (`major.minor.patch`), `null` when either side
     * is not semver-like.
     */
    fun compare(a: String, b: String): Int? {
        val left = parse(a) ?: return null
        val right = parse(b) ?: return null
        for (index in 0..2) {
            val delta = left[index].compareTo(right[index])
            if (delta != 0) return delta
        }
        return 0
    }

    private fun parse(version: String): List<Int>? {
        val core = version.substringBefore('-')
        val parts = core.split('.')
        if (parts.size != 3) return null
        val numbers = parts.map { it.toIntOrNull() ?: return null }
        return numbers
    }

    /** Human-readable line for a finding. */
    fun describe(finding: Finding): String = when (finding) {
        is Finding.Ahead ->
            "${finding.file}: ${finding.plugin} pinned ${finding.version} > published ${finding.expected}"
        is Finding.Obsolete ->
            "${finding.file}: ${finding.plugin} pinned ${finding.version} < published ${finding.expected}"
        is Finding.Snapshot ->
            "${finding.file}: ${finding.plugin} pinned ${finding.version} (SNAPSHOT forbidden)"
        is Finding.Invalid ->
            "${finding.file}: ${finding.plugin} pinned ${finding.version} (invalid semver)"
    }
}
