package build

import javax.inject.Inject

open class FunctionalTestConventionsExtension @Inject constructor() {
    var sourceSetName: String = "functionalTest"
    var additionalDependencies: List<String> = emptyList()
}
