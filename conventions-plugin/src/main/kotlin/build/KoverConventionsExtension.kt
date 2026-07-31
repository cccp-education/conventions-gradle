package build

import javax.inject.Inject

open class KoverConventionsExtension @Inject constructor() {
    var enabled: Boolean = false
    var threshold: Double? = null
}
