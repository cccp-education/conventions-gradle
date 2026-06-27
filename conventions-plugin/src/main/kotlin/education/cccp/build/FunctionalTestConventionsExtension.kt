package build

import javax.inject.Inject

open class FunctionalTestConventionsExtension @Inject constructor() {
    var additionalDependencies: List<String> = emptyList()
}
