package com.familytree.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FamilyMemberTest {

    @Test
    fun fullName_combinesLastNameAndFirstName() {
        val member = FamilyMember(
            id = "TEST-001",
            lastName = "张",
            firstName = "三"
        )
        assertEquals("张三", member.fullName)
    }

    @Test
    fun defaultGender_isMale() {
        val member = FamilyMember(
            id = "TEST-002",
            lastName = "李",
            firstName = "四"
        )
        assertEquals("male", member.gender)
    }

    @Test
    fun optionalFields_areNullByDefault() {
        val member = FamilyMember(
            id = "TEST-003",
            lastName = "王",
            firstName = "五"
        )
        assertNull(member.birthDate)
        assertNull(member.birthPlace)
        assertNull(member.deathDate)
        assertNull(member.deathPlace)
        assertNull(member.fatherId)
        assertNull(member.motherId)
        assertNull(member.notes)
    }

    @Test
    fun copy_preservesIdAndChangesOtherFields() {
        val original = FamilyMember(
            id = "ABCD-EFG",
            lastName = "赵",
            firstName = "六",
            gender = "male",
            birthDate = "1990-01-01"
        )
        val updated = original.copy(
            firstName = "七",
            birthPlace = "北京"
        )
        assertEquals("ABCD-EFG", updated.id)
        assertEquals("赵", updated.lastName)
        assertEquals("七", updated.firstName)
        assertEquals("北京", updated.birthPlace)
        assertEquals("1990-01-01", updated.birthDate)
    }

    @Test
    fun fullName_worksWithLongNames() {
        val member = FamilyMember(
            id = "TEST-004",
            lastName = "欧阳",
            firstName = "修文"
        )
        assertEquals("欧阳修文", member.fullName)
    }

    @Test
    fun familyRelationships_canBeSet() {
        val father = FamilyMember(id = "FATH-001", lastName = "张", firstName = "大")
        val mother = FamilyMember(id = "MOTH-001", lastName = "李", firstName = "美")
        val child = FamilyMember(
            id = "CHLD-001",
            lastName = "张",
            firstName = "小",
            fatherId = father.id,
            motherId = mother.id
        )
        assertEquals("FATH-001", child.fatherId)
        assertEquals("MOTH-001", child.motherId)
    }

    @Test
    fun memberWithAllFields_isCreatedCorrectly() {
        val member = FamilyMember(
            id = "FULL-001",
            lastName = "陈",
            firstName = "明",
            gender = "male",
            birthDate = "1950-03-15",
            birthPlace = "上海",
            deathDate = "2020-12-01",
            deathPlace = "北京",
            fatherId = "PRNT-001",
            motherId = "PRNT-002",
            notes = "家族第三代长子"
        )
        assertEquals("陈明", member.fullName)
        assertEquals("male", member.gender)
        assertEquals("1950-03-15", member.birthDate)
        assertEquals("上海", member.birthPlace)
        assertEquals("2020-12-01", member.deathDate)
        assertEquals("北京", member.deathPlace)
        assertEquals("PRNT-001", member.fatherId)
        assertEquals("PRNT-002", member.motherId)
        assertEquals("家族第三代长子", member.notes)
    }
}
