package com.familytree.app.data.gedcom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GedcomParserTest {

    private lateinit var parser: GedcomParser

    @Before
    fun setup() {
        parser = GedcomParser()
    }

    @Test
    fun parse_sampleFamily_returns6Members() {
        val input = javaClass.classLoader!!.getResourceAsStream("sample_family.ged")!!
        val result = parser.parse(input)

        assertEquals(6, result.members.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun parse_sampleFamily_correctNames() {
        val input = javaClass.classLoader!!.getResourceAsStream("sample_family.ged")!!
        val result = parser.parse(input)

        val names = result.members.map { "${it.lastName}${it.firstName}" }.toSet()
        assertTrue(names.contains("张大明"))
        assertTrue(names.contains("李秀英"))
        assertTrue(names.contains("张小明"))
        assertTrue(names.contains("张小红"))
        assertTrue(names.contains("王美丽"))
        assertTrue(names.contains("张天一"))
    }

    @Test
    fun parse_sampleFamily_correctGender() {
        val input = javaClass.classLoader!!.getResourceAsStream("sample_family.ged")!!
        val result = parser.parse(input)

        val byName = result.members.associateBy { "${it.lastName}${it.firstName}" }
        assertEquals("male", byName["张大明"]!!.gender)
        assertEquals("female", byName["李秀英"]!!.gender)
        assertEquals("male", byName["张小明"]!!.gender)
        assertEquals("female", byName["张小红"]!!.gender)
        assertEquals("female", byName["王美丽"]!!.gender)
        assertEquals("male", byName["张天一"]!!.gender)
    }

    @Test
    fun parse_sampleFamily_correctDates() {
        val input = javaClass.classLoader!!.getResourceAsStream("sample_family.ged")!!
        val result = parser.parse(input)

        val byName = result.members.associateBy { "${it.lastName}${it.firstName}" }

        assertEquals("1950-03-15", byName["张大明"]!!.birthDate)
        assertEquals("2020-06-20", byName["张大明"]!!.deathDate)
        assertEquals("1952-07-08", byName["李秀英"]!!.birthDate)
        assertNull(byName["李秀英"]!!.deathDate)
        assertEquals("1975-01-01", byName["张小明"]!!.birthDate)
        assertEquals("2000-02-10", byName["张天一"]!!.birthDate)
    }

    @Test
    fun parse_sampleFamily_correctPlaces() {
        val input = javaClass.classLoader!!.getResourceAsStream("sample_family.ged")!!
        val result = parser.parse(input)

        val byName = result.members.associateBy { "${it.lastName}${it.firstName}" }

        assertEquals("北京", byName["张大明"]!!.birthPlace)
        assertEquals("上海", byName["张大明"]!!.deathPlace)
        assertEquals("南京", byName["李秀英"]!!.birthPlace)
        assertEquals("杭州", byName["王美丽"]!!.birthPlace)
    }

    @Test
    fun parse_sampleFamily_correctParentRelationships() {
        val input = javaClass.classLoader!!.getResourceAsStream("sample_family.ged")!!
        val result = parser.parse(input)

        val byName = result.members.associateBy { "${it.lastName}${it.firstName}" }
        val father = byName["张大明"]!!
        val mother = byName["李秀英"]!!
        val son = byName["张小明"]!!
        val daughter = byName["张小红"]!!
        val wife = byName["王美丽"]!!
        val grandson = byName["张天一"]!!

        assertNull(father.fatherId)
        assertNull(father.motherId)
        assertNull(mother.fatherId)
        assertNull(mother.motherId)

        assertEquals(father.id, son.fatherId)
        assertEquals(mother.id, son.motherId)
        assertEquals(father.id, daughter.fatherId)
        assertEquals(mother.id, daughter.motherId)

        assertEquals(son.id, grandson.fatherId)
        assertEquals(wife.id, grandson.motherId)
    }

    @Test
    fun parse_sampleFamily_correctNotes() {
        val input = javaClass.classLoader!!.getResourceAsStream("sample_family.ged")!!
        val result = parser.parse(input)

        val byName = result.members.associateBy { "${it.lastName}${it.firstName}" }
        assertEquals("家族族长", byName["张大明"]!!.notes)
        assertEquals("长子", byName["张小明"]!!.notes)
        assertEquals("第三代长孙", byName["张天一"]!!.notes)
        assertNull(byName["李秀英"]!!.notes)
    }

    @Test
    fun parse_minimal_singlePerson() {
        val input = javaClass.classLoader!!.getResourceAsStream("minimal.ged")!!
        val result = parser.parse(input)

        assertEquals(1, result.members.size)
        val person = result.members[0]
        assertEquals("Doe", person.lastName)
        assertEquals("John", person.firstName)
        assertEquals("male", person.gender)
        assertEquals("1990", person.birthDate)
    }

    @Test
    fun parse_nameFormats_variousNameStyles() {
        val input = javaClass.classLoader!!.getResourceAsStream("name_formats.ged")!!
        val result = parser.parse(input)

        assertEquals(3, result.members.size)
        val zhao = result.members.find { it.lastName == "赵" }
        assertNotNull(zhao)
        assertEquals("志强", zhao!!.firstName)
        assertEquals("1985-03", zhao.birthDate)

        val sun = result.members.find { it.lastName == "孙" }
        assertNotNull(sun)
        assertEquals("", sun!!.firstName)
        assertEquals("2000", sun.birthDate)

        val noSurname = result.members.find { it.firstName == "无姓" }
        assertNotNull(noSurname)
        assertEquals("", noSurname!!.lastName)
    }

    @Test
    fun parse_emptyInput_returnsEmptyList() {
        val result = parser.parse("")
        assertTrue(result.members.isEmpty())
    }

    @Test
    fun parse_headerOnly_returnsEmptyList() {
        val input = """
            0 HEAD
            1 SOUR Test
            1 GEDC
            2 VERS 5.5
            2 FORM LINEAGE-LINKED
            1 CHAR UTF-8
            0 TRLR
        """.trimIndent()

        val result = parser.parse(input)
        assertTrue(result.members.isEmpty())
    }

    @Test
    fun parse_inlineString_works() {
        val input = """
            0 HEAD
            1 SOUR Test
            1 GEDC
            2 VERS 5.5
            2 FORM LINEAGE-LINKED
            1 CHAR UTF-8
            0 @I1@ INDI
            1 NAME 测试 /姓/
            1 SEX F
            1 BIRT
            2 DATE 25 DEC 1995
            2 PLAC 深圳
            0 TRLR
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals(1, result.members.size)
        assertEquals("姓", result.members[0].lastName)
        assertEquals("测试", result.members[0].firstName)
        assertEquals("female", result.members[0].gender)
        assertEquals("1995-12-25", result.members[0].birthDate)
        assertEquals("深圳", result.members[0].birthPlace)
    }

    @Test
    fun parse_uniqueIdsGenerated() {
        val input = javaClass.classLoader!!.getResourceAsStream("sample_family.ged")!!
        val result = parser.parse(input)

        val ids = result.members.map { it.id }.toSet()
        assertEquals(result.members.size, ids.size)

        for (id in ids) {
            assertTrue("ID应符合 XXXX-XXX 格式: $id", id.matches(Regex("[A-Z2-9]{4}-[A-Z2-9]{3}")))
        }
    }

    @Test
    fun parse_noteWithContinuation() {
        val input = """
            0 HEAD
            1 SOUR Test
            1 GEDC
            2 VERS 5.5
            2 FORM LINEAGE-LINKED
            1 CHAR UTF-8
            0 @I1@ INDI
            1 NAME 测试 /人/
            1 SEX M
            1 NOTE 第一行备注
            2 CONT 第二行备注
            2 CONC 续接文字
            0 TRLR
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals(1, result.members.size)
        assertEquals("第一行备注\n第二行备注续接文字", result.members[0].notes)
    }

    @Test
    fun convertGedcomDate_fullDate() {
        assertEquals("1990-01-01", GedcomParser.convertGedcomDate("1 JAN 1990"))
        assertEquals("2020-12-25", GedcomParser.convertGedcomDate("25 DEC 2020"))
        assertEquals("1985-06-15", GedcomParser.convertGedcomDate("15 JUN 1985"))
    }

    @Test
    fun convertGedcomDate_monthYear() {
        assertEquals("1990-01", GedcomParser.convertGedcomDate("JAN 1990"))
        assertEquals("2020-12", GedcomParser.convertGedcomDate("DEC 2020"))
    }

    @Test
    fun convertGedcomDate_yearOnly() {
        assertEquals("1990", GedcomParser.convertGedcomDate("1990"))
        assertEquals("2000", GedcomParser.convertGedcomDate("2000"))
    }

    @Test
    fun convertGedcomDate_emptyString() {
        assertNull(GedcomParser.convertGedcomDate(""))
        assertNull(GedcomParser.convertGedcomDate("   "))
    }

    @Test
    fun convertGedcomDate_singleDigitDay() {
        assertEquals("1990-03-01", GedcomParser.convertGedcomDate("1 MAR 1990"))
        assertEquals("1990-07-09", GedcomParser.convertGedcomDate("9 JUL 1990"))
    }

    @Test
    fun parseLine_standardLine() {
        val line = parser.parseLine("1 NAME John /Doe/")
        assertNotNull(line)
        assertEquals(1, line!!.level)
        assertNull(line.xref)
        assertEquals("NAME", line.tag)
        assertEquals("John /Doe/", line.value)
    }

    @Test
    fun parseLine_xrefLine() {
        val line = parser.parseLine("0 @I1@ INDI")
        assertNotNull(line)
        assertEquals(0, line!!.level)
        assertEquals("@I1@", line.xref)
        assertEquals("INDI", line.tag)
    }

    @Test
    fun parseLine_tagOnly() {
        val line = parser.parseLine("1 BIRT")
        assertNotNull(line)
        assertEquals(1, line!!.level)
        assertEquals("BIRT", line.tag)
        assertNull(line.value)
    }

    @Test
    fun parseLine_withBom() {
        val line = parser.parseLine("\uFEFF0 HEAD")
        assertNotNull(line)
        assertEquals(0, line!!.level)
        assertEquals("HEAD", line.tag)
    }
}
