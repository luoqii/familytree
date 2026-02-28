package com.familytree.app.ui.screens

import com.familytree.app.data.model.FamilyMember
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenGenerationCountTest {

    @Test
    fun calculateGenerationCount_emptyMembers_returnsZero() {
        assertEquals(0, calculateGenerationCount(emptyList()))
    }

    @Test
    fun calculateGenerationCount_withoutRoots_returnsOne() {
        val members = listOf(
            FamilyMember(id = "A", lastName = "张", firstName = "甲", fatherId = "B"),
            FamilyMember(id = "B", lastName = "李", firstName = "乙", fatherId = "A")
        )

        assertEquals(1, calculateGenerationCount(members))
    }

    @Test
    fun calculateGenerationCount_multiGenerationFamily_returnsMaxDepth() {
        val members = listOf(
            FamilyMember(id = "R1", lastName = "王", firstName = "一代"),
            FamilyMember(id = "C1", lastName = "王", firstName = "二代", fatherId = "R1"),
            FamilyMember(id = "G1", lastName = "王", firstName = "三代", fatherId = "C1"),
            FamilyMember(id = "R2", lastName = "李", firstName = "一代"),
            FamilyMember(id = "C2", lastName = "李", firstName = "二代", fatherId = "R2")
        )

        assertEquals(3, calculateGenerationCount(members))
    }
}
