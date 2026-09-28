package build

/**
 * Pure policy deciding whether Maven publications must be signed (DDD — no
 * Gradle types, no I/O).
 *
 * Signing requires a GPG key. A **test** CI job has none imported, so it must
 * skip signing — otherwise `publishToMavenLocal` (a build step) fails.
 *
 * A **release** job, however, *does* import a GPG key and must sign: Maven
 * Central rejects unsigned artefacts, so publishing from CI cannot reuse the
 * "skip on CI" rule. The publish job signals its intent explicitly by exporting
 * [PUBLISH_ENV_VAR] (`CCCP_PUBLISH=true`), which is the only CI case where
 * signing is required.
 *
 * A `-SNAPSHOT` version is never signed.
 */
object SigningPolicy {

    /** Exported by the workflow job that imports a GPG key (release). */
    const val PUBLISH_ENV_VAR = "CCCP_PUBLISH"

    const val CI_ENV_VAR = "CI"

    /**
     * @return `true` when publications must be signed: a `-SNAPSHOT` is never
     *   signed; a release job ([isPublishing]) always signs (it holds a key);
     *   a plain CI test job does not sign (no key); a local build signs.
     */
    fun shouldSign(isCi: Boolean, isPublishing: Boolean, version: String): Boolean {
        if (version.endsWith("-SNAPSHOT")) return false
        return isPublishing || !isCi
    }
}
