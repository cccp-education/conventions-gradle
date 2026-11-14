package build

/**
 * GBL-014 — the publication channel is a **property of the build**, never a
 * branch.
 *
 * One artefact = one version = one commit. The same tag `vX.Y.Z` serves:
 *
 * - [CENTRAL] — `education.cccp:<artifact>` (+ `education.cccp.*` plugin
 *   markers), Maven Central (nmcp) / Gradle Plugin Portal, GPG signed;
 * - [JITPACK] — `com.github.cccp-education.<repo>:<artifact>` (JitPack
 *   relabels the POM), tag-driven build, signing disabled (`CI=true`).
 *
 * `jitpack` is the default while the Sonatype quota is closed (reset
 * 2026-12-01). `central` is explicit opt-in.
 *
 * Pure domain: no Gradle types, no I/O.
 */
enum class PublicationChannel(val id: String) {
    JITPACK("jitpack"),
    CENTRAL("central"),
    ;

    companion object {
        /** Gradle property name (`-Pchannel=central|jitpack`). */
        const val PROPERTY = "channel"

        /** Default while Central is closed; never a hidden `central` switch. */
        val DEFAULT: PublicationChannel = JITPACK

        /**
         * Parses the raw `-Pchannel` value.
         *
         * @throws IllegalArgumentException for an unknown value: a silent
         *   fallback to `jitpack` would publish to the wrong channel.
         */
        fun parse(raw: String?): PublicationChannel {
            val value = raw?.trim()?.lowercase()
            if (value.isNullOrEmpty()) return DEFAULT
            return entries.firstOrNull { it.id == value }
                ?: throw IllegalArgumentException(
                    "Unknown publication channel '$raw'. " +
                        "Expected 'central' or 'jitpack' (-P$PROPERTY=central|jitpack).",
                )
        }
    }
}
