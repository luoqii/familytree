package com.familytree.app.data.gedcom

import com.familytree.app.data.model.FamilyMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GedcomExporterTest {

    private lateinit var exporter: GedcomExporter

    @Before
    fun setup() {
        exporter = GedcomExporter()
    }

    @Test
    fun export_emptyList_hasHeaderAndTrailer() {
        val result = exporter.export(emptyList())

        assertTrue(result.contains("0 HEAD"))
        assertTrue(result.contains("1 GEDC"))
        assertTrue(result.contains("2 VERS 5.5"))
        assertTrue(result.contains("2 FORM LINEAGE-LINKED"))
        assertTrue(result.contains("1 CHAR UTF-8"))
        assertTrue(result.trimEnd().endsWith("0 TRLR"))
    }

    @Test
    fun export_singlePerson_correctFormat() {
        val member = FamilyMember(
            id = "ABCD-EFG",
            lastName = "张",
            firstName = "三",
            gender = "male",
            birthDate = "1990-01-15",
            birthPlace = "北京"
        )

        val result = exporter.export(listOf(member))

        assertTrue(result.contains("0 @I1@ INDI"))
        assertTrue(result.contains("1 NAME 三 /张/"))
        assertTrue(result.contains("2 GIVN 三"))
        assertTrue(result.contains("2 SURN 张"))
        assertTrue(result.contains("1 SEX M"))
        assertTrue(result.contains("1 BIRT"))
        assertTrue(result.contains("2 DATE 15 JAN 1990"))
        assertTrue(result.contains("2 PLAC 北京"))
    }

    @Test
    fun export_femalePerson_correctSex() {
        val member = FamilyMember(
            id = "ABCD-EFG",
            lastName = "李",
            firstName = "梅",
            gender = "female"
        )

        val result = exporter.export(listOf(member))
        assertTrue(result.contains("1 SEX F"))
    }

    @Test
    fun export_withDeath_includesDeathRecord() {
        val member = FamilyMember(
            id = "ABCD-EFG",
            lastName = "王",
            firstName = "五",
            gender = "male",
            deathDate = "2020-06-20",
            deathPlace = "上海"
        )

        val result = exporter.export(listOf(member))
        assertTrue(result.contains("1 DEAT"))
        assertTrue(result.contains("2 DATE 20 JUN 2020"))
        assertTrue(result.contains("2 PLAC 上海"))
    }

    @Test
    fun export_withNotes_includesNoteRecord() {
        val member = FamilyMember(
            id = "ABCD-EFG",
            lastName = "张",
            firstName = "一",
            gender = "male",
            notes = "这是备注"
        )

        val result = exporter.export(listOf(member))
        assertTrue(result.contains("1 NOTE 这是备注"))
    }

    @Test
    fun export_multilineNotes_usesCONT() {
        val member = FamilyMember(
            id = "ABCD-EFG",
            lastName = "张",
            firstName = "一",
            gender = "male",
            notes = "第一行\n第二行\n第三行"
        )

        val result = exporter.export(listOf(member))
        assertTrue(result.contains("1 NOTE 第一行"))
        assertTrue(result.contains("2 CONT 第二行"))
        assertTrue(result.contains("2 CONT 第三行"))
    }

    @Test
    fun export_familyRelationships_createsFamRecords() {
        val father = FamilyMember(id = "F001-AAA", lastName = "张", firstName = "大", gender = "male")
        val mother = FamilyMember(id = "M001-BBB", lastName = "李", firstName = "美", gender = "female")
        val child = FamilyMember(
            id = "C001-CCC", lastName = "张", firstName = "小",
            gender = "male", fatherId = "F001-AAA", motherId = "M001-BBB"
        )

        val result = exporter.export(listOf(father, mother, child))

        assertTrue("应包含 FAM 记录", result.contains("FAM"))
        assertTrue("应包含 HUSB 引用", result.contains("1 HUSB"))
        assertTrue("应包含 WIFE 引用", result.contains("1 WIFE"))
        assertTrue("应包含 CHIL 引用", result.contains("1 CHIL"))
        assertTrue("子女应有 FAMC 引用", result.contains("1 FAMC"))
        assertTrue("父亲应有 FAMS 引用", result.contains("1 FAMS"))
    }

    @Test
    fun export_multipleFamilies_createsMultipleFamRecords() {
        val grandpa = FamilyMember(id = "G001-AAA", lastName = "张", firstName = "祖", gender = "male")
        val grandma = FamilyMember(id = "G002-BBB", lastName = "王", firstName = "祖母", gender = "female")
        val father = FamilyMember(
            id = "F001-CCC", lastName = "张", firstName = "父",
            gender = "male", fatherId = "G001-AAA", motherId = "G002-BBB"
        )
        val mother = FamilyMember(id = "M001-DDD", lastName = "李", firstName = "母", gender = "female")
        val child = FamilyMember(
            id = "C001-EEE", lastName = "张", firstName = "子",
            gender = "male", fatherId = "F001-CCC", motherId = "M001-DDD"
        )

        val result = exporter.export(listOf(grandpa, grandma, father, mother, child))

        val famCount = Regex("0 @F\\d+@ FAM").findAll(result).count()
        assertEquals("应有2个家庭记录", 2, famCount)
    }

    @Test
    fun export_noBirthOrDeath_omitsRecords() {
        val member = FamilyMember(
            id = "ABCD-EFG",
            lastName = "张",
            firstName = "三",
            gender = "male"
        )

        val result = exporter.export(listOf(member))
        assertFalse(result.contains("1 BIRT"))
        assertFalse(result.contains("1 DEAT"))
    }

    @Test
    fun export_gedcomVersion55() {
        val result = exporter.export(emptyList())
        assertTrue(result.contains("2 VERS 5.5"))
        assertTrue(result.contains("2 FORM LINEAGE-LINKED"))
    }

    @Test
    fun buildFamilies_groupsSiblings() {
        val child1 = FamilyMember(
            id = "C001", lastName = "张", firstName = "一",
            gender = "male", fatherId = "F001", motherId = "M001"
        )
        val child2 = FamilyMember(
            id = "C002", lastName = "张", firstName = "二",
            gender = "female", fatherId = "F001", motherId = "M001"
        )

        val families = exporter.buildFamilies(listOf(child1, child2))
        assertEquals(1, families.size)
        assertEquals(2, families[0].childIds.size)
        assertTrue(families[0].childIds.contains("C001"))
        assertTrue(families[0].childIds.contains("C002"))
    }

    @Test
    fun buildFamilies_differentParents_separateFamilies() {
        val child1 = FamilyMember(
            id = "C001", lastName = "张", firstName = "一",
            gender = "male", fatherId = "F001", motherId = "M001"
        )
        val child2 = FamilyMember(
            id = "C002", lastName = "李", firstName = "二",
            gender = "male", fatherId = "F002", motherId = "M002"
        )

        val families = exporter.buildFamilies(listOf(child1, child2))
        assertEquals(2, families.size)
    }

    @Test
    fun buildFamilies_noParents_noFamilies() {
        val member = FamilyMember(
            id = "M001", lastName = "张", firstName = "一", gender = "male"
        )

        val families = exporter.buildFamilies(listOf(member))
        assertTrue(families.isEmpty())
    }

    @Test
    fun convertToGedcomDate_fullDate() {
        assertEquals("15 JAN 1990", GedcomExporter.convertToGedcomDate("1990-01-15"))
        assertEquals("25 DEC 2020", GedcomExporter.convertToGedcomDate("2020-12-25"))
        assertEquals("1 MAR 1985", GedcomExporter.convertToGedcomDate("1985-03-01"))
    }

    @Test
    fun convertToGedcomDate_monthYear() {
        assertEquals("JAN 1990", GedcomExporter.convertToGedcomDate("1990-01"))
        assertEquals("DEC 2020", GedcomExporter.convertToGedcomDate("2020-12"))
    }

    @Test
    fun convertToGedcomDate_yearOnly() {
        assertEquals("1990", GedcomExporter.convertToGedcomDate("1990"))
        assertEquals("2000", GedcomExporter.convertToGedcomDate("2000"))
    }

    @Test
    fun convertToGedcomDate_empty() {
        assertEquals(null, GedcomExporter.convertToGedcomDate(""))
        assertEquals(null, GedcomExporter.convertToGedcomDate("   "))
    }
}
