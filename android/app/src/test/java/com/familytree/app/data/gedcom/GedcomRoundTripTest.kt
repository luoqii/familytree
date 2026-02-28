package com.familytree.app.data.gedcom

import com.familytree.app.data.model.FamilyMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 导出 -> 导入 往返测试
 * 确保导出后再导入的数据与原始数据一致
 */
class GedcomRoundTripTest {

    @Test
    fun roundTrip_singlePerson_preservesData() {
        val original = FamilyMember(
            id = "ABCD-EFG",
            lastName = "张",
            firstName = "三",
            gender = "male",
            birthDate = "1990-01-15",
            birthPlace = "北京",
            deathDate = "2060-12-25",
            deathPlace = "上海",
            notes = "测试备注"
        )

        val exporter = GedcomExporter()
        val gedcom = exporter.export(listOf(original))

        val parser = GedcomParser()
        val result = parser.parse(gedcom)

        assertEquals(1, result.members.size)
        val imported = result.members[0]

        assertEquals(original.lastName, imported.lastName)
        assertEquals(original.firstName, imported.firstName)
        assertEquals(original.gender, imported.gender)
        assertEquals(original.birthDate, imported.birthDate)
        assertEquals(original.birthPlace, imported.birthPlace)
        assertEquals(original.deathDate, imported.deathDate)
        assertEquals(original.deathPlace, imported.deathPlace)
        assertEquals(original.notes, imported.notes)
    }

    @Test
    fun roundTrip_familyWithRelationships_preservesStructure() {
        val father = FamilyMember(
            id = "F001-AAA", lastName = "张", firstName = "大",
            gender = "male", birthDate = "1960-05-10", birthPlace = "北京"
        )
        val mother = FamilyMember(
            id = "M001-BBB", lastName = "李", firstName = "美",
            gender = "female", birthDate = "1962-08-20", birthPlace = "南京"
        )
        val son = FamilyMember(
            id = "S001-CCC", lastName = "张", firstName = "小",
            gender = "male", birthDate = "1985-03-15", birthPlace = "北京",
            fatherId = "F001-AAA", motherId = "M001-BBB"
        )
        val daughter = FamilyMember(
            id = "D001-DDD", lastName = "张", firstName = "红",
            gender = "female", birthDate = "1988-07-22", birthPlace = "北京",
            fatherId = "F001-AAA", motherId = "M001-BBB"
        )

        val originals = listOf(father, mother, son, daughter)

        val exporter = GedcomExporter()
        val gedcom = exporter.export(originals)

        val parser = GedcomParser()
        val result = parser.parse(gedcom)

        assertEquals(4, result.members.size)

        val byName = result.members.associateBy { "${it.lastName}${it.firstName}" }

        val importedFather = byName["张大"]
        val importedMother = byName["李美"]
        val importedSon = byName["张小"]
        val importedDaughter = byName["张红"]

        assertNotNull(importedFather)
        assertNotNull(importedMother)
        assertNotNull(importedSon)
        assertNotNull(importedDaughter)

        assertNull(importedFather!!.fatherId)
        assertNull(importedFather.motherId)
        assertNull(importedMother!!.fatherId)
        assertNull(importedMother.motherId)

        assertEquals(importedFather.id, importedSon!!.fatherId)
        assertEquals(importedMother.id, importedSon.motherId)
        assertEquals(importedFather.id, importedDaughter!!.fatherId)
        assertEquals(importedMother.id, importedDaughter.motherId)
    }

    @Test
    fun roundTrip_threeGenerations_preservesAllRelationships() {
        val grandpa = FamilyMember(
            id = "GP01-AAA", lastName = "张", firstName = "祖",
            gender = "male", birthDate = "1930-01-01"
        )
        val grandma = FamilyMember(
            id = "GM01-BBB", lastName = "王", firstName = "祖母",
            gender = "female", birthDate = "1932-06-15"
        )
        val father = FamilyMember(
            id = "FA01-CCC", lastName = "张", firstName = "父",
            gender = "male", birthDate = "1960-03-20",
            fatherId = "GP01-AAA", motherId = "GM01-BBB"
        )
        val mother = FamilyMember(
            id = "MO01-DDD", lastName = "李", firstName = "母",
            gender = "female", birthDate = "1962-11-30"
        )
        val child = FamilyMember(
            id = "CH01-EEE", lastName = "张", firstName = "子",
            gender = "male", birthDate = "1990-07-04",
            fatherId = "FA01-CCC", motherId = "MO01-DDD"
        )

        val originals = listOf(grandpa, grandma, father, mother, child)

        val exporter = GedcomExporter()
        val gedcom = exporter.export(originals)

        val parser = GedcomParser()
        val result = parser.parse(gedcom)

        assertEquals(5, result.members.size)
        assertTrue(result.errors.isEmpty())

        val byName = result.members.associateBy { "${it.lastName}${it.firstName}" }

        val iGrandpa = byName["张祖"]!!
        val iGrandma = byName["王祖母"]!!
        val iFather = byName["张父"]!!
        val iChild = byName["张子"]!!

        assertEquals(iGrandpa.id, iFather.fatherId)
        assertEquals(iGrandma.id, iFather.motherId)
        assertEquals(iFather.id, iChild.fatherId)
    }

    @Test
    fun roundTrip_femalePerson_preservesGender() {
        val original = FamilyMember(
            id = "ABCD-EFG",
            lastName = "王",
            firstName = "美丽",
            gender = "female",
            birthDate = "1995-04-20"
        )

        val exporter = GedcomExporter()
        val gedcom = exporter.export(listOf(original))

        val parser = GedcomParser()
        val result = parser.parse(gedcom)

        assertEquals("female", result.members[0].gender)
    }

    @Test
    fun roundTrip_importSampleThenExport_preservesMemberCount() {
        val sampleInput = javaClass.classLoader!!.getResourceAsStream("sample_family.ged")!!
        val parser = GedcomParser()
        val imported = parser.parse(sampleInput)

        val exporter = GedcomExporter()
        val exported = exporter.export(imported.members)

        val reimported = parser.parse(exported)
        assertEquals(imported.members.size, reimported.members.size)
    }
}
