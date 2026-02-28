package com.familytree.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonIdGeneratorTest {

    @Test
    fun generate_returnsCorrectFormat() {
        val id = PersonIdGenerator.generate()
        val regex = Regex("^[A-Z2-9]{4}-[A-Z2-9]{3}$")
        assertTrue("ID '$id' should match XXXX-XXX format", regex.matches(id))
    }

    @Test
    fun generate_returnsUniqueIds() {
        val ids = (1..100).map { PersonIdGenerator.generate() }.toSet()
        assertEquals("100 generated IDs should all be unique", 100, ids.size)
    }

    @Test
    fun generate_doesNotContainConfusingChars() {
        val confusingChars = setOf('O', '0', 'I', '1', 'L')
        repeat(100) {
            val id = PersonIdGenerator.generate()
            for (ch in id) {
                if (ch != '-') {
                    assertTrue(
                        "ID should not contain confusing char '$ch'",
                        ch !in confusingChars
                    )
                }
            }
        }
    }

    @Test
    fun generate_hasCorrectLength() {
        val id = PersonIdGenerator.generate()
        assertEquals("ID should be 8 characters (XXXX-XXX)", 8, id.length)
    }

    @Test
    fun generate_twoConsecutiveCalls_returnDifferentIds() {
        val id1 = PersonIdGenerator.generate()
        val id2 = PersonIdGenerator.generate()
        assertNotEquals("Two consecutive IDs should be different", id1, id2)
    }
}
