package build

/**
 * Pure policy deciding whether Maven publications must be signed (DDD — no
 * Gradle types, no I/O).
 *
 * Signing requires a GPG key; CI runners and local `-SNAPSHOT` builds have
 * none, so they must skip it. Kept out of [PublishingConventionsPlugin] so the
 * decision is unit-testable without a Gradle build, and so the CI branch can be
 * exercised deterministically (a runner exports `CI=true`).
 */
object SigningPolicy {

    const val CI_ENV_VAR = "CI"

    /**
     * @return `true` when publications must be signed: not on CI, and never for
     *   a `-SNAPSHOT` version.
     */
    fun shouldSign(isCi: Boolean, version: String): Boolean =
        !isCi && !version.endsWith("-SNAPSHOT")
}
