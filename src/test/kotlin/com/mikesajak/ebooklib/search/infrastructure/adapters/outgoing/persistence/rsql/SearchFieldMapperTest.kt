package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SearchFieldMapperTest {
    private lateinit var searchFieldMapper: SearchFieldMapper

    @BeforeEach
    fun setUp() {
        searchFieldMapper = SearchFieldMapper()
    }

    @Test
    fun `mapToEntityField should return correct entity field for valid domain field`() {
        assertEquals("title", searchFieldMapper.mapToEntityField("title"))
        assertEquals("authors.firstName", searchFieldMapper.mapToEntityField("authors.firstName"))
    }

    @Test
    fun `mapToEntityField should throw IllegalArgumentException for invalid domain field`() {
        assertThrows<IllegalArgumentException> {
            searchFieldMapper.mapToEntityField("invalidField")
        }
    }

    @Test
    fun `isFieldAllowed should return true for allowed field`() {
        assertTrue(searchFieldMapper.isFieldAllowed("title"))
        assertTrue(searchFieldMapper.isFieldAllowed("authors.firstName"))
    }

    @Test
    fun `isFieldAllowed should return false for disallowed field`() {
        assertFalse(searchFieldMapper.isFieldAllowed("nonExistentField"))
    }

    @Test
    fun `getAllowedFields should return correct set of allowed fields`() {
        val expectedFields = setOf(
                "title", "description", "authors.firstName", "authors.lastName",
                "author.description", "author.bio", "series.name", "series.volume"
        )
        assertEquals(expectedFields, searchFieldMapper.getAllowedFields())
    }
}
