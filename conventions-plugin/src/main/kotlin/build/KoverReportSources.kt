package build

/**
 * Pure domain concept describing which source sets are measured by the Kover
 * conventions plugin.
 *
 * Extracted from [KoverConventionsPlugin] so that the inclusion policy is
 * unit-testable without a Gradle build. The report measures the production code
 * (`main`) together with its functional tests (`functionalTest`); the unit test
 * source set is excluded by Kover itself, and the Playwright-driven `e2eTest`
 * source set is not part of `check` so it must not distort the threshold.
 */
object KoverReportSources {

    const val MAIN = "main"
    const val FUNCTIONAL_TEST = "functionalTest"

    /** The source sets whose classes are measured, in declaration order. */
    val included: List<String> = listOf(MAIN, FUNCTIONAL_TEST)

    /**
     * @return `true` when classes of [sourceSetName] belong to the coverage
     *   measured by the conventions plugin.
     */
    fun isIncluded(sourceSetName: String): Boolean = sourceSetName in included
}
