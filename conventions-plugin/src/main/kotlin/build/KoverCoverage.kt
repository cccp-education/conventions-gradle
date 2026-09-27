package build

/**
 * Pure domain concept for Kover coverage reports.
 *
 * Extracted from [KoverConventionsPlugin] so that the coverage computation is
 * unit-testable without a Gradle build. The instruction counters in a Kover XML
 * report are hierarchical: every enclosing element repeats the aggregate of its
 * children, so summing *all* `INSTRUCTION` counters yields the same ratio as
 * reading the root counter — the ratio is invariant under the hierarchy.
 */
object KoverCoverage {

    private val INSTRUCTION_COUNTER =
        Regex("""<counter type="INSTRUCTION" missed="(\d+)" covered="(\d+)"/>""")

    data class Summary(val missed: Long, val covered: Long) {
        val total: Long get() = missed + covered
        val percent: Double get() = if (total > 0) covered.toDouble() / total * 100 else 0.0
    }

    /**
     * Sums every `INSTRUCTION` counter found in [reportXml].
     *
     * @return the aggregated [Summary]; `missed = covered = 0` when the report
     *   carries no instruction counter.
     */
    fun summarize(reportXml: String): Summary {
        var missed = 0L
        var covered = 0L
        for (match in INSTRUCTION_COUNTER.findAll(reportXml)) {
            missed += match.groupValues[1].toLong()
            covered += match.groupValues[2].toLong()
        }
        return Summary(missed, covered)
    }

    /**
     * @return `true` when the instruction coverage in [reportXml] is greater
     *   than or equal to [threshold] (as a percentage, e.g. `85.0`).
     */
    fun meetsThreshold(reportXml: String, threshold: Double): Boolean =
        summarize(reportXml).percent >= threshold
}
