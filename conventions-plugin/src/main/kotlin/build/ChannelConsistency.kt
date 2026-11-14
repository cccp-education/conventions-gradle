package build

/**
 * GBL-014 — static consistency of a `settings.gradle(.kts)` file with the
 * selected [PublicationChannel].
 *
 * The channel is a projection, not a fork: a `jitpack` build declares the
 * JitPack repository and the `useModule(...)` shim (JitPack does not serve the
 * plugin markers at the plugin id's groupId); a `central` build declares
 * neither and resolves the `education.cccp.*` markers from Central / Portal.
 *
 * Pure domain: the file content is injected, so the policy is unit-testable
 * without a Gradle build and without reading another repository's tree.
 */
object ChannelConsistency {

    data class Finding(val rule: Rule, val message: String) {
        enum class Rule {
            MISSING_JITPACK_REPOSITORY,
            MISSING_SHIM,
            FORBIDDEN_JITPACK_REPOSITORY,
            FORBIDDEN_SHIM,
        }
    }

    internal const val JITPACK_REPOSITORY = "https://jitpack.io"
    internal const val SHIM_MODULE = "com.github.cccp-education.conventions-gradle"
    internal const val SHIM_CALL = "usemodule("

    /**
     * Audits [settings] against [channel].
     *
     * Comment lines are ignored so the explanatory `// CNV-16 …` comments that
     * every consumer keeps do not create false positives (notably in `central`
     * mode, where any JitPack reference is a hard violation).
     */
    fun auditSettings(settings: String, channel: PublicationChannel): List<Finding> {
        val code = stripCommentLines(settings).lowercase()
        val hasJitpackRepository = code.contains(JITPACK_REPOSITORY)
        val hasShim = code.contains(SHIM_CALL) ||
            (code.contains("conventions-gradle") && code.contains("com.github.cccp-education"))

        return when (channel) {
            PublicationChannel.JITPACK -> buildList {
                if (!hasJitpackRepository) {
                    add(
                        Finding(
                            Finding.Rule.MISSING_JITPACK_REPOSITORY,
                            "channel=jitpack requires the JitPack repository ($JITPACK_REPOSITORY)",
                        ),
                    )
                }
                if (!hasShim) {
                    add(
                        Finding(
                            Finding.Rule.MISSING_SHIM,
                            "channel=jitpack requires the useModule shim to $SHIM_MODULE",
                        ),
                    )
                }
            }

            PublicationChannel.CENTRAL -> buildList {
                if (hasJitpackRepository) {
                    add(
                        Finding(
                            Finding.Rule.FORBIDDEN_JITPACK_REPOSITORY,
                            "channel=central must not declare the JitPack repository ($JITPACK_REPOSITORY)",
                        ),
                    )
                }
                if (hasShim) {
                    add(
                        Finding(
                            Finding.Rule.FORBIDDEN_SHIM,
                            "channel=central must not declare the JitPack useModule shim",
                        ),
                    )
                }
            }
        }
    }

    /** Human-readable line for a finding. */
    fun describe(finding: Finding): String = "${finding.rule}: ${finding.message}"

    private fun stripCommentLines(text: String): String =
        text.lineSequence()
            .filterNot { it.trimStart().startsWith("//") }
            .joinToString("\n")
}
