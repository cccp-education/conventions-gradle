package build

/**
 * GBL-014 — tasks that belong to **one** channel only.
 *
 * `publishPlugins` (Gradle Plugin Portal) and the nmcp aggregation tasks
 * (`publishAggregationToCentralPortal`, `publishAllPublicationsToCentralPortal`)
 * are the `central` channel. Running them from a `jitpack` build would publish
 * the same version to a second channel — violating "one tag = one commit" and
 * "one channel = one catalogue".
 *
 * Pure policy: returns a violation message, or `null` when the task is allowed.
 */
object PublishChannelGuard {

    private val CENTRAL_ONLY_PREFIXES = listOf(
        "publishAggregationToCentralPortal",
        "publishAllPublicationsToCentralPortal",
    )

    fun isCentralOnly(taskName: String): Boolean =
        taskName == "publishPlugins" || CENTRAL_ONLY_PREFIXES.any { taskName.startsWith(it) }

    fun violation(channel: PublicationChannel, taskName: String): String? {
        if (!isCentralOnly(taskName)) return null
        if (channel == PublicationChannel.CENTRAL) return null
        return "Task '$taskName' is a central-channel task but the build runs " +
            "channel=${channel.id}. Use -P${PublicationChannel.PROPERTY}=central " +
            "(one tag = one commit; never publish the same version to both channels)."
    }
}
