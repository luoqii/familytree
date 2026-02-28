package com.familytree.app.data.gedcom

import com.familytree.app.data.model.FamilyMember
import com.familytree.app.data.model.PersonIdGenerator
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * GEDCOM 5.5 格式解析器
 *
 * 将 GEDCOM 5.5 文本解析为 FamilyMember 列表。
 * 处理 HEAD、INDI、FAM、TRLR 等记录类型，映射 GEDCOM 引用 ID
 * 到应用内部的 PersonIdGenerator ID。
 */
class GedcomParser {

    data class ParseResult(
        val members: List<FamilyMember>,
        val errors: List<String> = emptyList()
    )

    internal data class GedcomLine(
        val level: Int,
        val xref: String?,
        val tag: String,
        val value: String?
    )

    private data class RawIndividual(
        val xref: String,
        var givenName: String? = null,
        var surname: String? = null,
        var sex: String? = null,
        var birthDate: String? = null,
        var birthPlace: String? = null,
        var deathDate: String? = null,
        var deathPlace: String? = null,
        var note: String? = null,
        val familyChildXrefs: MutableList<String> = mutableListOf(),
        val familySpouseXrefs: MutableList<String> = mutableListOf()
    )

    private data class RawFamily(
        val xref: String,
        var husbandXref: String? = null,
        var wifeXref: String? = null,
        val childXrefs: MutableList<String> = mutableListOf()
    )

    fun parse(input: String): ParseResult {
        return parse(input.byteInputStream())
    }

    fun parse(inputStream: InputStream): ParseResult {
        val lines = mutableListOf<GedcomLine>()
        val errors = mutableListOf<String>()

        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            var lineNumber = 0
            reader.forEachLine { rawLine ->
                lineNumber++
                val trimmed = rawLine.trim()
                if (trimmed.isNotEmpty()) {
                    val parsed = parseLine(trimmed)
                    if (parsed != null) {
                        lines.add(parsed)
                    } else {
                        errors.add("第 $lineNumber 行格式无效: $trimmed")
                    }
                }
            }
        }

        val individuals = mutableMapOf<String, RawIndividual>()
        val families = mutableMapOf<String, RawFamily>()

        extractRecords(lines, individuals, families, errors)

        return buildResult(individuals, families, errors)
    }

    internal fun parseLine(line: String): GedcomLine? {
        val trimmed = line.trimStart('\uFEFF').trim()
        if (trimmed.isEmpty()) return null

        val parts = trimmed.split(" ", limit = 3)
        if (parts.isEmpty()) return null

        val level = parts[0].toIntOrNull() ?: return null

        if (parts.size < 2) return null

        val second = parts[1]
        return if (second.startsWith("@") && second.endsWith("@")) {
            val xref = second
            val tag = if (parts.size >= 3) parts[2].split(" ", limit = 2)[0] else ""
            val value = if (parts.size >= 3) {
                val tagAndValue = parts[2].split(" ", limit = 2)
                if (tagAndValue.size > 1) tagAndValue[1] else null
            } else null
            GedcomLine(level, xref, tag, value)
        } else {
            val tag = second
            val value = if (parts.size >= 3) parts[2] else null
            GedcomLine(level, null, tag, value)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun extractRecords(
        lines: List<GedcomLine>,
        individuals: MutableMap<String, RawIndividual>,
        families: MutableMap<String, RawFamily>,
        errors: MutableList<String>
    ) {
        var currentIndi: RawIndividual? = null
        var currentFam: RawFamily? = null
        var currentSubTag: String? = null

        for (line in lines) {
            when (line.level) {
                0 -> {
                    currentIndi = null
                    currentFam = null
                    currentSubTag = null

                    when (line.tag) {
                        "INDI" -> {
                            val xref = line.xref ?: continue
                            currentIndi = RawIndividual(xref = xref)
                            individuals[xref] = currentIndi
                        }
                        "FAM" -> {
                            val xref = line.xref ?: continue
                            currentFam = RawFamily(xref = xref)
                            families[xref] = currentFam
                        }
                    }
                }
                1 -> {
                    currentSubTag = line.tag
                    when {
                        currentIndi != null -> parseIndiLevel1(line, currentIndi)
                        currentFam != null -> parseFamLevel1(line, currentFam)
                    }
                }
                2 -> {
                    when {
                        currentIndi != null -> parseIndiLevel2(line, currentIndi, currentSubTag)
                    }
                }
            }
        }
    }

    private fun parseIndiLevel1(line: GedcomLine, indi: RawIndividual) {
        when (line.tag) {
            "NAME" -> {
                val value = line.value ?: return
                val nameMatch = Regex("(.*?)\\s*/(.+?)/(.*)").find(value)
                if (nameMatch != null) {
                    val given = nameMatch.groupValues[1].trim()
                    val surname = nameMatch.groupValues[2].trim()
                    if (given.isNotEmpty()) indi.givenName = given
                    if (surname.isNotEmpty()) indi.surname = surname
                } else {
                    indi.givenName = value.trim()
                }
            }
            "SEX" -> {
                indi.sex = line.value?.trim()
            }
            "BIRT", "DEAT" -> { /* handled at level 2 */ }
            "NOTE" -> {
                indi.note = line.value
            }
            "FAMC" -> {
                line.value?.let { indi.familyChildXrefs.add(it.trim()) }
            }
            "FAMS" -> {
                line.value?.let { indi.familySpouseXrefs.add(it.trim()) }
            }
        }
    }

    private fun parseIndiLevel2(line: GedcomLine, indi: RawIndividual, parentTag: String?) {
        when (parentTag) {
            "BIRT" -> {
                when (line.tag) {
                    "DATE" -> indi.birthDate = line.value?.trim()
                    "PLAC" -> indi.birthPlace = line.value?.trim()
                }
            }
            "DEAT" -> {
                when (line.tag) {
                    "DATE" -> indi.deathDate = line.value?.trim()
                    "PLAC" -> indi.deathPlace = line.value?.trim()
                }
            }
            "NAME" -> {
                when (line.tag) {
                    "GIVN" -> indi.givenName = line.value?.trim()
                    "SURN" -> indi.surname = line.value?.trim()
                }
            }
            "NOTE" -> {
                when (line.tag) {
                    "CONT" -> {
                        indi.note = (indi.note ?: "") + "\n" + (line.value ?: "")
                    }
                    "CONC" -> {
                        indi.note = (indi.note ?: "") + (line.value ?: "")
                    }
                }
            }
        }
    }

    private fun parseFamLevel1(line: GedcomLine, fam: RawFamily) {
        when (line.tag) {
            "HUSB" -> fam.husbandXref = line.value?.trim()
            "WIFE" -> fam.wifeXref = line.value?.trim()
            "CHIL" -> line.value?.trim()?.let { fam.childXrefs.add(it) }
        }
    }

    private fun buildResult(
        individuals: Map<String, RawIndividual>,
        families: Map<String, RawFamily>,
        errors: MutableList<String>
    ): ParseResult {
        val xrefToId = mutableMapOf<String, String>()
        for (xref in individuals.keys) {
            xrefToId[xref] = PersonIdGenerator.generate()
        }

        val parentMap = buildParentMap(families)

        val members = individuals.map { (xref, indi) ->
            val internalId = xrefToId[xref]!!
            val fatherXref = parentMap[xref]?.first
            val motherXref = parentMap[xref]?.second

            FamilyMember(
                id = internalId,
                lastName = indi.surname ?: "",
                firstName = indi.givenName ?: "",
                gender = when (indi.sex?.uppercase()) {
                    "M" -> "male"
                    "F" -> "female"
                    else -> "male"
                },
                birthDate = indi.birthDate?.let { convertGedcomDate(it) },
                birthPlace = indi.birthPlace,
                deathDate = indi.deathDate?.let { convertGedcomDate(it) },
                deathPlace = indi.deathPlace,
                fatherId = fatherXref?.let { xrefToId[it] },
                motherId = motherXref?.let { xrefToId[it] },
                notes = indi.note
            )
        }

        return ParseResult(members, errors)
    }

    /**
     * 从 FAM 记录构建 子女 -> (父亲xref, 母亲xref) 映射
     */
    private fun buildParentMap(
        families: Map<String, RawFamily>
    ): Map<String, Pair<String?, String?>> {
        val parentMap = mutableMapOf<String, Pair<String?, String?>>()

        for ((_, fam) in families) {
            for (childXref in fam.childXrefs) {
                val existing = parentMap[childXref]
                val fatherXref = fam.husbandXref ?: existing?.first
                val motherXref = fam.wifeXref ?: existing?.second
                parentMap[childXref] = Pair(fatherXref, motherXref)
            }
        }

        return parentMap
    }

    companion object {
        private val MONTH_MAP = mapOf(
            "JAN" to "01", "FEB" to "02", "MAR" to "03",
            "APR" to "04", "MAY" to "05", "JUN" to "06",
            "JUL" to "07", "AUG" to "08", "SEP" to "09",
            "OCT" to "10", "NOV" to "11", "DEC" to "12"
        )

        /**
         * GEDCOM 日期格式 (DD MON YYYY, MON YYYY, YYYY) -> YYYY-MM-DD
         */
        fun convertGedcomDate(gedcomDate: String): String? {
            val trimmed = gedcomDate.trim()
            if (trimmed.isEmpty()) return null

            val fullMatch = Regex("(\\d{1,2})\\s+(\\w{3})\\s+(\\d{4})").find(trimmed)
            if (fullMatch != null) {
                val day = fullMatch.groupValues[1].padStart(2, '0')
                val month = MONTH_MAP[fullMatch.groupValues[2].uppercase()] ?: return trimmed
                val year = fullMatch.groupValues[3]
                return "$year-$month-$day"
            }

            val monthYearMatch = Regex("(\\w{3})\\s+(\\d{4})").find(trimmed)
            if (monthYearMatch != null) {
                val month = MONTH_MAP[monthYearMatch.groupValues[1].uppercase()] ?: return trimmed
                val year = monthYearMatch.groupValues[2]
                return "$year-$month"
            }

            val yearMatch = Regex("^(\\d{4})$").find(trimmed)
            if (yearMatch != null) {
                return yearMatch.groupValues[1]
            }

            return trimmed
        }
    }
}
