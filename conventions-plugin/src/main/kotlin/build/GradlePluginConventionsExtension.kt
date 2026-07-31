package build

import javax.inject.Inject

open class GradlePluginConventionsExtension @Inject constructor() {
    var enableDynamicAgentLoading: Boolean = true
    var maxHeapSize: String? = null
    var parallelExecution: Boolean = false
    var fixAnnotationsConflict: Boolean = false
}
