package com.familytree.app.data.gedcom

import com.familytree.app.data.model.FamilyMember

/**
 * GEDCOM 5.5 格式导出器
 *
 * 将 FamilyMember 列表导出为标准 GEDCOM 5.5 格式文本。
 * 为每个个人生成 INDI 记录，并根据亲子关系自动构建 FAM 记录。
 */
class GedcomExporter {

    fun export(members: List<FamilyMember>): String {
        val sb = StringBuilder()

        writeHeader(sb)

        val idToXref = buildIdMapping(members)

        val families = buildFamilies(members)

        for (member in members) {
            writeIndividual(sb, member, idToXref, families)
        }

        for ((index, family) in families.withIndex()) {
            writeFamily(sb, index + 1, family, idToXref)
        }

        sb.appendLine("0 TRLR")

        return sb.toString()
    }

    private fun writeHeader(sb: StringBuilder) {
        sb.appendLine("0 HEAD")
        sb.appendLine("1 SOUR FamilyTreeApp")
        sb.appendLine("2 VERS 1.0.0")
        sb.appendLine("2 NAME 家族树")
        sb.appendLine("1 DEST DISK")
        sb.appendLine("1 GEDC")
        sb.appendLine("2 VERS 5.5")
        sb.appendLine("2 FORM LINEAGE-LINKED")
        sb.appendLine("1 CHAR UTF-8")
    }

    private fun buildIdMapping(members: List<FamilyMember>): Map<String, String> {
        return members.mapIndexed { index, member ->
            member.id to "@I${index + 1}@"
        }.toMap()
    }

    data class FamilyRecord(
        val fatherId: String?,
        val motherId: String?,
        val childIds: List<String>
    )

    internal fun buildFamilies(members: List<FamilyMember>): List<FamilyRecord> {
        val familyKey = mutableMapOf<Pair<String?, String?>, MutableList<String>>()

        for (member in members) {
            if (member.fatherId != null || member.motherId != null) {
                val key = Pair(member.fatherId, member.motherId)
                familyKey.getOrPut(key) { mutableListOf() }.add(member.id)
            }
        }

        return familyKey.map { (key, childIds) ->
            FamilyRecord(
                fatherId = key.first,
                motherId = key.second,
                childIds = childIds
            )
        }
    }

    private fun getFamilyXrefsForMember(
        member: FamilyMember,
        families: List<FamilyRecord>
    ): Pair<List<Int>, List<Int>> {
        val familyChild = mutableListOf<Int>()
        val familySpouse = mutableListOf<Int>()

        families.forEachIndexed { index, fam ->
            if (member.id in fam.childIds) {
                familyChild.add(index + 1)
            }
            if (member.id == fam.fatherId || member.id == fam.motherId) {
                familySpouse.add(index + 1)
            }
        }

        return Pair(familyChild, familySpouse)
    }

    private fun writeIndividual(
        sb: StringBuilder,
        member: FamilyMember,
        idToXref: Map<String, String>,
        families: List<FamilyRecord>
    ) {
        val xref = idToXref[member.id] ?: return

        sb.appendLine("0 $xref INDI")
        sb.appendLine("1 NAME ${member.firstName} /${member.lastName}/")
        sb.appendLine("2 GIVN ${member.firstName}")
        sb.appendLine("2 SURN ${member.lastName}")
        sb.appendLine("1 SEX ${if (member.gender == "female") "F" else "M"}")

        if (member.birthDate != null || member.birthPlace != null) {
            sb.appendLine("1 BIRT")
            member.birthDate?.let { date ->
                convertToGedcomDate(date)?.let { sb.appendLine("2 DATE $it") }
            }
            member.birthPlace?.let { sb.appendLine("2 PLAC $it") }
        }

        if (member.deathDate != null || member.deathPlace != null) {
            sb.appendLine("1 DEAT")
            member.deathDate?.let { date ->
                convertToGedcomDate(date)?.let { sb.appendLine("2 DATE $it") }
            }
            member.deathPlace?.let { sb.appendLine("2 PLAC $it") }
        }

        val (familyChild, familySpouse) = getFamilyXrefsForMember(member, families)
        for (famIdx in familyChild) {
            sb.appendLine("1 FAMC @F${famIdx}@")
        }
        for (famIdx in familySpouse) {
            sb.appendLine("1 FAMS @F${famIdx}@")
        }

        member.notes?.let { note ->
            val lines = note.split("\n")
            if (lines.isNotEmpty()) {
                sb.appendLine("1 NOTE ${lines[0]}")
                for (i in 1 until lines.size) {
                    sb.appendLine("2 CONT ${lines[i]}")
                }
            }
        }
    }

    private fun writeFamily(
        sb: StringBuilder,
        index: Int,
        family: FamilyRecord,
        idToXref: Map<String, String>
    ) {
        sb.appendLine("0 @F${index}@ FAM")
        family.fatherId?.let { id ->
            idToXref[id]?.let { sb.appendLine("1 HUSB $it") }
        }
        family.motherId?.let { id ->
            idToXref[id]?.let { sb.appendLine("1 WIFE $it") }
        }
        for (childId in family.childIds) {
            idToXref[childId]?.let { sb.appendLine("1 CHIL $it") }
        }
    }

    companion object {
        private val MONTH_NAMES = arrayOf(
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
            "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
        )

        /**
         * YYYY-MM-DD -> DD MON YYYY (GEDCOM格式)
         * YYYY-MM -> MON YYYY
         * YYYY -> YYYY
         */
        fun convertToGedcomDate(date: String): String? {
            val trimmed = date.trim()
            if (trimmed.isEmpty()) return null

            val fullMatch = Regex("(\\d{4})-(\\d{2})-(\\d{2})").find(trimmed)
            if (fullMatch != null) {
                val year = fullMatch.groupValues[1]
                val monthIdx = fullMatch.groupValues[2].toIntOrNull()?.minus(1) ?: return trimmed
                val day = fullMatch.groupValues[3].toInt()
                if (monthIdx in 0..11) {
                    return "$day ${MONTH_NAMES[monthIdx]} $year"
                }
                return trimmed
            }

            val monthYearMatch = Regex("(\\d{4})-(\\d{2})").find(trimmed)
            if (monthYearMatch != null) {
                val year = monthYearMatch.groupValues[1]
                val monthIdx = monthYearMatch.groupValues[2].toIntOrNull()?.minus(1) ?: return trimmed
                if (monthIdx in 0..11) {
                    return "${MONTH_NAMES[monthIdx]} $year"
                }
                return trimmed
            }

            val yearMatch = Regex("^(\\d{4})$").find(trimmed)
            if (yearMatch != null) {
                return yearMatch.groupValues[1]
            }

            return trimmed
        }
    }
}
