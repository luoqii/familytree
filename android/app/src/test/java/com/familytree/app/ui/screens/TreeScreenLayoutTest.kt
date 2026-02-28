package com.familytree.app.ui.screens

import com.familytree.app.data.model.FamilyMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeScreenLayoutTest {

    @Test
    fun buildLayout_largeTree_respectsRenderLimit() {
        val members = buildLinearFamily(size = 5000)

        val result = buildLayout(members, maxNodes = 300)

        assertEquals(300, result.renderedNodeCount)
        assertTrue(result.isTruncated)
        assertTrue(result.roots.isNotEmpty())
    }

    @Test
    fun buildLayout_smallTree_rendersAllNodes() {
        val members = buildLinearFamily(size = 80)

        val result = buildLayout(members, maxNodes = 300)

        assertEquals(80, result.renderedNodeCount)
        assertFalse(result.isTruncated)
    }

    private fun buildLinearFamily(size: Int): List<FamilyMember> {
        val members = ArrayList<FamilyMember>(size)
        for (index in 0 until size) {
            val id = "M-${index.toString().padStart(5, '0')}"
            val fatherId = if (index == 0) null else "M-${(index - 1).toString().padStart(5, '0')}"
            members += FamilyMember(
                id = id,
                lastName = "姓",
                firstName = "名$index",
                fatherId = fatherId
            )
        }
        return members
    }
}
