package build

import javax.inject.Inject

open class KoverConventionsExtension @Inject constructor() {
    var enabled: Boolean = false
    var threshold: Double? = null

    /**
     * Source sets measured by the coverage report. Defaults to
     * [KoverReportSources.included] (`main` + `functionalTest`). A consumer
     * measuring only production code sets `listOf("main")`.
     */
    var includedSourceSets: List<String> = KoverReportSources.included

    /**
     * Whether the HTML report is generated on `check`. A consumer whose
     * TestKit tests cannot be instrumented (on-fly agent limitation) disables
     * it explicitly.
     */
    var htmlOnCheck: Boolean = true

    /** Whether the XML report is generated on `check`. */
    var xmlOnCheck: Boolean = true

    /**
     * Optional override of the XML report path, relative to the build
     * directory (e.g. `reports/kover/xml/report.xml`). The threshold check
     * reads the configured path, never a hardcoded one.
     */
    var xmlFile: String? = null

    /** Optional override of the HTML report directory, relative to the build directory. */
    var htmlDir: String? = null
}
