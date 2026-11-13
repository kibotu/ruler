package com.kibotu.ruler.analysis.proguard

import com.kibotu.ruler.analysis.dependency.DependencyComponent
import com.kibotu.ruler.model.ProguardRuleFile
import com.kibotu.ruler.model.RuleFlag
import com.kibotu.ruler.model.RuleScope
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Reads the ProGuard/R8 rules that dependencies bundle, following the documented packaging
 * locations (developer.android.com — "Optimization for library authors"):
 *
 * - AAR: `proguard.txt` at the root, where AGP merged the consumer rules, plus the targeted
 *   R8 rules under `META-INF/com.android.tools/r8-…/` inside `classes.jar`.
 * - JAR: the consumer rules under `META-INF/proguard/`, plus targeted R8 rules under
 *   `META-INF/com.android.tools/r8-…/`.
 *
 * If a dependency ships targeted R8 rules, AGP uses those and ignores the legacy locations.
 * Which targeted directory applies depends on the R8 version of the consuming build, so every
 * `r8-from-<X>-upto-<Y>` directory is reported with its range in the origin. The rules of
 * project modules are declared in their build files and never packaged, so they are not found
 * here.
 */
class ProguardRuleExtractor {

    /** @return Component names mapped to the rule files their archives bundle. */
    fun parse(archives: List<RuleArchive>): Map<String, List<ProguardRuleFile>> =
        archives.groupBy { DependencyComponent.of(it.component).name }
            .mapValues { (_, archives) ->
                val files = archives.flatMap(::parseArchive)
                val namespace = commonPackage(archives.flatMap(::packagesOf))
                files.map { analyzed(it, namespace) }.sortedByDescending { it.ruleCount }
            }
            .filterValues { it.isNotEmpty() }

    private fun parseArchive(archive: RuleArchive): List<ProguardRuleFile> = when {
        archive.file.extension.equals("aar", ignoreCase = true) -> aarRules(archive)
        archive.file.extension.equals("jar", ignoreCase = true) -> jarRules(archive)
        else -> emptyList()
    }

    /**
     * Measures a file's rules against the package of the component that ships it. R8 applies a
     * consumer rule to the whole app, so a rule that names someone else's package is the
     * library author's rule changing another team's code. Without classes there is no namespace
     * to measure against, and nothing is flagged.
     */
    private fun analyzed(file: ProguardRuleFile, namespace: String?): ProguardRuleFile {
        val flags = RuleParser.parse(file.content).mapNotNull { rule ->
            scopeOf(rule, namespace)?.let { RuleFlag(rule.line, rule.directive.take(MAX_DIRECTIVE), it) }
        }
        return file.copy(namespace = namespace, flags = flags.ifEmpty { null })
    }

    private fun scopeOf(rule: ClassRule, namespace: String?): RuleScope? {
        val scopes = rule.patterns.mapNotNull { pattern ->
            val prefix = RuleParser.literalPrefix(pattern)
            when {
                prefix.isNotEmpty() ->
                    RuleScope.OUTSIDE_NAMESPACE.takeIf { namespace != null && !prefix.startsWith("$namespace.") }
                pattern in ALL && rule.pinsClasses && !rule.constrained -> RuleScope.ALL_CLASSES
                else -> RuleScope.CROSS_CUTTING
            }
        }
        return RuleScope.entries.firstOrNull { it in scopes }
    }

    /** The package of every class an archive holds; for an AAR, those of its `classes.jar`. */
    private fun packagesOf(archive: RuleArchive): List<String> = when {
        archive.file.extension.equals("aar", ignoreCase = true) -> ZipFile(archive.file).use { aar ->
            aar.getEntry(CLASSES_JAR)?.let { classesJar ->
                ZipInputStream(aar.getInputStream(classesJar)).use { packages(generateSequence { it.nextEntry }) }
            }.orEmpty()
        }
        archive.file.extension.equals("jar", ignoreCase = true) ->
            ZipFile(archive.file).use { packages(it.entries().asSequence()) }
        else -> emptyList()
    }

    private fun packages(entries: Sequence<ZipEntry>): List<String> = entries
        .map { it.name }
        // Multi-release copies and module descriptors are not part of the namespace.
        .filter { it.endsWith(".class") && it.contains('/') && !it.startsWith("META-INF/") }
        .map { it.substringBeforeLast('/').replace('/', '.') }
        .distinct()
        .toList()

    /** The longest package prefix, in whole segments, that all packages share. */
    private fun commonPackage(packages: List<String>): String? =
        packages.map { it.split('.') }.reduceOrNull { common, segments ->
            common.zip(segments).takeWhile { (a, b) -> a == b }.map { it.first }
        }?.joinToString(".")?.ifEmpty { null }

    private fun aarRules(archive: RuleArchive): List<ProguardRuleFile> = ZipFile(archive.file).use { aar ->
        buildList {
            aar.entries().asSequence()
                .filter { entry -> !entry.isDirectory && CONSUMER_LOCATIONS.any { it(entry) } }
                .forEach { entry -> ruleFile(aar.getInputStream(entry), entry.name, consumerOrigin)?.let(::add) }
            // The targeted R8 rules of an AAR travel inside its classes.jar.
            aar.getEntry(CLASSES_JAR)?.let { addAll(nestedRules(aar.getInputStream(it))) }
        }
    }

    private fun jarRules(archive: RuleArchive): List<ProguardRuleFile> = ZipFile(archive.file).use { jar ->
        jar.entries().asSequence()
            .filter { entry -> !entry.isDirectory && RULE_LOCATIONS.any { it(entry) } }
            .mapNotNull { entry -> ruleFile(jar.getInputStream(entry), entry.name, originOf(entry)) }
            .toList()
    }

    /** Reads the rule locations of a nested classes.jar without unpacking it to disk. */
    private fun nestedRules(classesJar: InputStream): List<ProguardRuleFile> =
        ZipInputStream(classesJar).use { jar ->
            generateSequence { jar.nextEntry }
                .filter { entry -> !entry.isDirectory && RULE_LOCATIONS.any { it(entry) } }
                .mapNotNull { entry -> ruleFile(jar, entry.name, originOf(entry)) }
                .toList()
        }

    private fun ruleFile(stream: InputStream, name: String, origin: String): ProguardRuleFile? {
        val content = stream.readBytes().toString(Charsets.UTF_8).trim()
        if (content.isEmpty()) return null
        return ProguardRuleFile(origin, name, content, content.lineSequence().count(::isRule))
    }

    private fun isRule(line: String): Boolean = line.trimStart().startsWith('-')

    /** `r8-from-8.0.0-upto-8.2.0` or the shortest prefix of it that a directory uses. */
    private fun originOf(entry: ZipEntry): String {
        val directory = entry.targetedR8Directory ?: return consumerOrigin
        if (directory == "r8") return "Targeted R8 rules (r8)"
        // "from-8.0.0-upto-8.2.0" reads better as words: "from 8.0.0 upto 8.2.0".
        val range = directory.removePrefix("r8-").split('-').joinToString(" ")
        return "Targeted R8 rules (r8-$range)"
    }

    companion object {
        private const val consumerOrigin = "Consumer rules"
        private const val CLASSES_JAR = "classes.jar"
        private const val MAX_DIRECTIVE = 200
        private val ALL = setOf("*", "**")

        /** Consumer rule files inside an AAR. AGP merges the declared ones into `proguard.txt`. */
        private val CONSUMER_LOCATIONS: List<(ZipEntry) -> Boolean> = listOf(
            // Legacy AAR location; the file name is fixed by the AAR format.
            { it.name == "proguard.txt" },
            // Some archives skip the AGP merge and ship the rules where a JAR would keep them.
            { it.name.startsWith("META-INF/proguard/") },
        )

        /** Every rule location inside a JAR, and the targeted R8 locations inside `classes.jar`. */
        private val RULE_LOCATIONS: List<(ZipEntry) -> Boolean> = listOf(
            { it.name.startsWith("META-INF/proguard/") },
            { it.name.startsWith("META-INF/com.android.tools/") && it.targetedR8Directory != null },
        )

        /** The `r8-from-<X>-upto-<Y>` directory a targeted rule sits in, or null if there is none. */
        private val ZipEntry.targetedR8Directory: String?
            get() = name.split('/').firstOrNull { it.startsWith("r8") }
    }
}
