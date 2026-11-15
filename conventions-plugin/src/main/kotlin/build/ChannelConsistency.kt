package build

/**
 * GBL-014 — **semantic** consistency of a `settings.gradle(.kts)` file with the
 * selected [PublicationChannel].
 *
 * The channel is a projection, not a fork: a `jitpack` build declares the
 * JitPack repository and the `useModule(...)` shim (JitPack does not serve the
 * plugin markers at the plugin id's groupId); a `central` build declares
 * neither and resolves the `education.cccp.*` markers from Central / Portal.
 *
 * Since GBL-014-5 every migrated consumer keeps **both** projections in the
 * same file, inside a channel-guarded block:
 *
 * ```kotlin
 * val publicationChannel = providers.gradleProperty("channel").getOrElse("jitpack")
 * if (publicationChannel == "jitpack") {
 *     useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.18")
 * }
 * ```
 *
 * A purely textual audit would therefore flag a correct `central` build (the
 * JitPack strings are still in the file). The audit is semantic: it masks
 * comments and string *contents* to locate the syntactic structure, then
 * ignores the branches the selected channel never executes —
 * the **then** branch of `if (<condition> … "jitpack")` and the **else**
 * branch of `if (<condition> … "central")`. A JitPack reference that survives
 * outside those projections in `central` mode is still a hard finding.
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

    private const val JITPACK = "jitpack"
    private const val CENTRAL = "central"

    /**
     * Audits [settings] against [channel].
     *
     * Comments are masked everywhere (the explanatory `// CNV-16 …` comments
     * that every consumer keeps must not create false positives), and the
     * channel-guarded branches are ignored in `central` mode.
     */
    fun auditSettings(settings: String, channel: PublicationChannel): List<Finding> {
        val visible = sanitize(settings, blankStrings = false)

        return when (channel) {
            PublicationChannel.JITPACK -> {
                val declared = visible.lowercase()
                buildList {
                    if (!declared.contains(JITPACK_REPOSITORY)) {
                        add(
                            Finding(
                                Finding.Rule.MISSING_JITPACK_REPOSITORY,
                                "channel=jitpack requires the JitPack repository ($JITPACK_REPOSITORY)",
                            ),
                        )
                    }
                    if (!hasShim(declared)) {
                        add(
                            Finding(
                                Finding.Rule.MISSING_SHIM,
                                "channel=jitpack requires the useModule shim to $SHIM_MODULE",
                            ),
                        )
                    }
                }
            }

            PublicationChannel.CENTRAL -> {
                val structural = sanitize(settings, blankStrings = true)
                val effective = blankCentralProjections(visible, structural).lowercase()
                buildList {
                    if (effective.contains(JITPACK_REPOSITORY)) {
                        add(
                            Finding(
                                Finding.Rule.FORBIDDEN_JITPACK_REPOSITORY,
                                "channel=central must not declare the JitPack repository " +
                                    "($JITPACK_REPOSITORY) outside the channel-guarded block",
                            ),
                        )
                    }
                    if (hasShim(effective)) {
                        add(
                            Finding(
                                Finding.Rule.FORBIDDEN_SHIM,
                                "channel=central must not declare the JitPack useModule shim " +
                                    "outside the channel-guarded block",
                            ),
                        )
                    }
                }
            }
        }
    }

    /** Human-readable line for a finding. */
    fun describe(finding: Finding): String = "${finding.rule}: ${finding.message}"

    /**
     * A consumer may use the module coordinates instead of the literal
     * `useModule(` call in a `when` branch; both shapes are the shim.
     */
    private fun hasShim(lowercased: String): Boolean =
        lowercased.contains(SHIM_CALL) ||
            (lowercased.contains("conventions-gradle") &&
                lowercased.contains("com.github.cccp-education"))

    // ── Channel-guarded branches ───────────────────────────────────────────

    private data class Guard(
        val conditionMentionsJitpack: Boolean,
        val thenBlock: IntRange,
        val elseBlock: IntRange?,
    )

    /**
     * Blanks the branches that `central` never executes:
     * the `then` block of a `"jitpack"` guard, and the `else` block of a
     * `"central"` guard (catalogue projection). [structural] has comments and
     * string contents blanked, so braces inside literals cannot unbalance the
     * scan; [visible] keeps the strings, so string ranges map 1:1 on both.
     */
    private fun blankCentralProjections(visible: String, structural: String): String {
        val chars = visible.toCharArray()
        var cursor = 0
        while (cursor < structural.length) {
            val guard = nextGuard(structural, visible, cursor) ?: break
            val ignored = if (guard.conditionMentionsJitpack) guard.thenBlock else guard.elseBlock
            if (ignored != null) {
                blank(chars, ignored)
                cursor = ignored.last + 1
            } else {
                cursor = guard.thenBlock.last + 1
            }
        }
        return String(chars)
    }

    private fun nextGuard(structural: String, visible: String, from: Int): Guard? {
        var index = from
        while (index < structural.length) {
            if (isKeyword(structural, index, "if")) {
                val openParen = skipWhitespace(structural, index + 2)
                if (openParen < structural.length && structural[openParen] == '(') {
                    val closeParen = matching(structural, openParen, '(', ')')
                    if (closeParen > 0) {
                        val condition = visible.substring(openParen + 1, closeParen)
                        val jitpack = condition.contains(JITPACK, ignoreCase = true)
                        val central = condition.contains(CENTRAL, ignoreCase = true)
                        if (jitpack || central) {
                            guard(structural, closeParen + 1, jitpack)?.let { return it }
                        }
                    }
                }
            }
            index++
        }
        return null
    }

    private fun guard(structural: String, afterCondition: Int, jitpack: Boolean): Guard? {
        val openBrace = skipWhitespace(structural, afterCondition)
        if (openBrace >= structural.length || structural[openBrace] != '{') return null
        val closeBrace = matching(structural, openBrace, '{', '}')
        if (closeBrace < 0) return null
        val thenBlock = openBrace..closeBrace

        val afterThen = skipWhitespace(structural, closeBrace + 1)
        val elseBlock = if (isKeyword(structural, afterThen, "else")) {
            val elseBrace = skipWhitespace(structural, afterThen + "else".length)
            if (elseBrace < structural.length && structural[elseBrace] == '{') {
                val closeElse = matching(structural, elseBrace, '{', '}')
                if (closeElse > 0) elseBrace..closeElse else null
            } else {
                null
            }
        } else {
            null
        }

        return Guard(jitpack, thenBlock, elseBlock)
    }

    private fun blank(chars: CharArray, range: IntRange) {
        for (index in range) {
            if (chars[index] != '\n') chars[index] = ' '
        }
    }

    // ── Lexical helpers ───────────────────────────────────────────────────

    private fun skipWhitespace(text: String, from: Int): Int {
        var index = from
        while (index < text.length && text[index].isWhitespace()) index++
        return index
    }

    private fun isKeyword(text: String, index: Int, keyword: String): Boolean {
        if (!text.startsWith(keyword, index)) return false
        val before = index - 1
        val after = index + keyword.length
        if (before >= 0 && isIdentifierPart(text[before])) return false
        if (after < text.length && isIdentifierPart(text[after])) return false
        return true
    }

    private fun isIdentifierPart(char: Char): Boolean = char.isLetterOrDigit() || char == '_'

    /** Index of the delimiter closing [openIndex], or `-1` when unbalanced. */
    private fun matching(text: String, openIndex: Int, open: Char, close: Char): Int {
        var depth = 0
        var index = openIndex
        while (index < text.length) {
            when (text[index]) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return index
                }
            }
            index++
        }
        return -1
    }

    /**
     * Masks line comments, block comments and, when [blankStrings] is set, the
     * content of string literals (escapes and raw strings included). Newlines
     * and offsets are preserved so both passes stay index-aligned.
     */
    private fun sanitize(text: String, blankStrings: Boolean): String {
        val out = StringBuilder(text.length)
        var index = 0
        var state = State.CODE
        while (index < text.length) {
            val char = text[index]
            when (state) {
                State.CODE -> when {
                    char == '/' && text.startsWith("//", index) -> {
                        state = State.LINE_COMMENT
                        out.append("  ")
                        index += 2
                    }

                    char == '/' && text.startsWith("/*", index) -> {
                        state = State.BLOCK_COMMENT
                        out.append("  ")
                        index += 2
                    }

                    text.startsWith("\"\"\"", index) -> {
                        state = State.RAW_STRING
                        out.append(if (blankStrings) "   " else "\"\"\"")
                        index += 3
                    }

                    char == '"' -> {
                        state = State.STRING
                        out.append(if (blankStrings) ' ' else char)
                        index++
                    }

                    char == '\'' -> {
                        state = State.CHAR_LITERAL
                        out.append(if (blankStrings) ' ' else char)
                        index++
                    }

                    else -> {
                        out.append(char)
                        index++
                    }
                }

                State.LINE_COMMENT -> {
                    if (char == '\n') {
                        state = State.CODE
                        out.append('\n')
                    } else {
                        out.append(' ')
                    }
                    index++
                }

                State.BLOCK_COMMENT -> {
                    if (text.startsWith("*/", index)) {
                        state = State.CODE
                        out.append("  ")
                        index += 2
                    } else {
                        out.append(if (char == '\n') '\n' else ' ')
                        index++
                    }
                }

                State.STRING, State.CHAR_LITERAL, State.RAW_STRING -> {
                    val closing = when (state) {
                        State.STRING -> "\""
                        State.CHAR_LITERAL -> "'"
                        else -> "\"\"\""
                    }
                    when {
                        state != State.RAW_STRING && char == '\\' && index + 1 < text.length -> {
                            out.append(if (blankStrings) "  " else "$char${text[index + 1]}")
                            index += 2
                        }

                        text.startsWith(closing, index) -> {
                            state = State.CODE
                            out.append(if (blankStrings) " ".repeat(closing.length) else closing)
                            index += closing.length
                        }

                        else -> {
                            out.append(if (blankStrings && char != '\n') ' ' else char)
                            index++
                        }
                    }
                }
            }
        }
        return out.toString()
    }

    private enum class State { CODE, LINE_COMMENT, BLOCK_COMMENT, STRING, CHAR_LITERAL, RAW_STRING }
}
