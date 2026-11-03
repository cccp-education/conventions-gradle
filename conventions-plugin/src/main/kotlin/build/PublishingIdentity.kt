package build

/**
 * POM identity carried by every publication: licence and developer.
 *
 * Pure domain (no Gradle type) so the fallback policy is unit-testable: a
 * blank override never erases the cccp default, it falls back to it. This is
 * what allows a non-cccp consumer (e.g. the proprietary payment-scaffold
 * providers) to reuse the `publishing` convention without inheriting the
 * Apache-2.0 licence by accident.
 */
data class PublishingIdentity(
    val licenseName: String,
    val licenseUrl: String,
    val developerId: String,
    val developerName: String,
    val developerEmail: String,
) {
    companion object {
        const val DEFAULT_LICENSE_NAME = "Apache-2.0"
        const val DEFAULT_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0.txt"
        const val DEFAULT_DEVELOPER_ID = "cccp-education"
        const val DEFAULT_DEVELOPER_NAME = "CCCP Education"
        const val DEFAULT_DEVELOPER_EMAIL = "cccp@cccp.education"

        val DEFAULT = PublishingIdentity(
            licenseName = DEFAULT_LICENSE_NAME,
            licenseUrl = DEFAULT_LICENSE_URL,
            developerId = DEFAULT_DEVELOPER_ID,
            developerName = DEFAULT_DEVELOPER_NAME,
            developerEmail = DEFAULT_DEVELOPER_EMAIL,
        )

        /**
         * Resolves the effective identity: every blank/absent override falls
         * back to its cccp default, and every value is trimmed.
         */
        fun resolve(
            licenseName: String? = null,
            licenseUrl: String? = null,
            developerId: String? = null,
            developerName: String? = null,
            developerEmail: String? = null,
        ): PublishingIdentity = PublishingIdentity(
            licenseName = licenseName.orDefault(DEFAULT_LICENSE_NAME),
            licenseUrl = licenseUrl.orDefault(DEFAULT_LICENSE_URL),
            developerId = developerId.orDefault(DEFAULT_DEVELOPER_ID),
            developerName = developerName.orDefault(DEFAULT_DEVELOPER_NAME),
            developerEmail = developerEmail.orDefault(DEFAULT_DEVELOPER_EMAIL),
        )

        private fun String?.orDefault(fallback: String): String =
            this?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
    }
}
