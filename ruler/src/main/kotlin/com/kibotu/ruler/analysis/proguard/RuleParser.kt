package com.kibotu.ruler.analysis.proguard

/** How long a reported directive may get, so a long class specification stays on one line. */
internal const val MAX_DIRECTIVE = 200

/** A class specification of a ProGuard/R8 directive, reduced to what name matching needs. */
internal data class ClassRule(
    /** 1-based line where the directive starts. */
    val line: Int,
    /** The directive up to its member block, on one line. */
    val directive: String,
    /**
     * Whether R8 has to keep the classes it matches, which is what makes an app bigger. Keeping
     * only members or names, `allowshrinking`, and rewrites like `-assumenosideeffects` keep none.
     */
    val pins: Boolean,
    /** Whether it applies to every matching class, rather than only the ones that have the listed members. */
    val wholeClass: Boolean,
    /** The class name patterns, with `/` written as `.` and negated patterns dropped. */
    val patterns: List<String>,
    /** Narrowed by `extends`, `implements` or an annotation, which name matching cannot resolve. */
    val constrained: Boolean,
    /** Only applies when the `-if` before it holds. */
    val conditional: Boolean = false,
)

/**
 * Finds the class specifications in a rules file. Only the directives that name classes are
 * kept; options like `-dontwarn` or `-optimizationpasses` have no class to match.
 */
internal object RuleParser {

    private val KEEP = setOf(
        "-keep", "-keepclassmembers", "-keepclasseswithmembers",
        "-keepnames", "-keepclassmembernames", "-keepclasseswithmembernames",
    )
    private val PINS = setOf("-keep", "-keepclasseswithmembers")
    private val WHOLE_CLASS = setOf("-keep", "-keepnames")
    private val REWRITE = setOf("-assumenosideeffects", "-assumevalues")
    private val CLASS_KEYWORDS = setOf("class", "interface", "enum", "@interface")
    private val HIERARCHY = setOf("extends", "implements")

    fun parse(content: String): List<ClassRule> {
        val rules = mutableListOf<ClassRule>()
        val lines = content.lines()
        var index = 0
        var conditional = false
        while (index < lines.size) {
            val start = index
            val first = lines[index++].substringBefore('#').trim()
            if (!first.startsWith("-")) continue
            // A member block may span lines, so read until its braces balance.
            val directive = StringBuilder(first)
            while (directive.count { it == '{' } > directive.count { it == '}' } && index < lines.size) {
                directive.append(' ').append(lines[index++].substringBefore('#').trim())
            }
            classRule(start + 1, directive.toString(), conditional)?.let(rules::add)
            conditional = first.startsWith("-if ")
        }
        return rules
    }

    private fun classRule(line: Int, directive: String, conditional: Boolean): ClassRule? {
        val head = directive.substringBefore('{').trim()
        val tokens = head.split(Regex("\\s+"))
        val options = tokens.first().split(',')
        val name = options.first()
        if (name !in KEEP && name !in REWRITE) return null

        val keyword = tokens.indexOfFirst { it in CLASS_KEYWORDS }
        if (keyword < 0) return null
        val annotated = tokens.take(keyword).any { it.startsWith("@") }
        val spec = tokens.drop(keyword + 1)
        val hierarchy = spec.indexOfFirst { it in HIERARCHY }
        val names = (if (hierarchy < 0) spec else spec.take(hierarchy)).joinToString(" ")

        val patterns = names.split(',').map { it.trim().replace('/', '.') }
            .filter { it.isNotEmpty() && !it.startsWith("!") }
        if (patterns.isEmpty()) return null
        return ClassRule(
            line = line,
            directive = head,
            pins = name in PINS && "allowshrinking" !in options,
            wholeClass = name in WHOLE_CLASS,
            patterns = patterns,
            constrained = annotated || hierarchy >= 0,
            conditional = conditional,
        )
    }

    /** The part of a pattern before its first wildcard, which every matching class starts with. */
    fun literalPrefix(pattern: String): String = pattern.takeWhile { it !in WILDCARDS }

    private val WILDCARDS = charArrayOf('*', '?', '%', '<')

    /** `*` stays within a package, `**` crosses packages, `?` is one character. */
    fun regexOf(pattern: String): Regex {
        val regex = StringBuilder()
        var i = 0
        while (i < pattern.length) {
            when (val c = pattern[i]) {
                '*' -> if (pattern.getOrNull(i + 1) == '*') { regex.append(".*"); i++ } else regex.append("[^.]*")
                '?' -> regex.append("[^.]")
                '%' -> regex.append("(?!)")
                '<' -> {
                    val end = pattern.indexOf('>', i)
                    if (end < 0) regex.append("\\<") else { regex.append(".*"); i = end }
                }
                else -> regex.append(Regex.escape(c.toString()))
            }
            i++
        }
        return Regex(regex.toString())
    }
}
