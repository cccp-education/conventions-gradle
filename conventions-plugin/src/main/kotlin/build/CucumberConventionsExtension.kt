package build

import javax.inject.Inject

data class CucumberTaskSpec(
    val name: String,
    val features: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val runnerClass: String? = null,
    val parallel: Boolean = false,
    val timeoutMinutes: Int? = null
)

open class CucumberConventionsExtension @Inject constructor() {
    var featuresDir: String = "src/test/resources/features"
    var scenariosDir: String = "src/test/scenarios"
    var cucumberTestTaskName: String = "cucumberTest"
    var parallel: Boolean = false
    var timeoutMinutes: Int? = null
    var additionalTasks: List<CucumberTaskSpec> = emptyList()

    /**
     * Whether the `CucumberTaskSpec` tasks declared in [additionalTasks] are
     * wired into `check`. Default `false` (backward compatible): a consumer with
     * many focused runners (e.g. capsule with 22) would otherwise see `check`
     * balloon, and those runners are already executed by the `full-tests` CI
     * workflow. Opt in explicitly when `check` must cover every runner.
     */
    var additionalTasksInCheck: Boolean = false
}
