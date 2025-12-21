package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SearchFieldMapperTest {
    private lateinit var searchFieldMapper: SearchFieldMapper

    @BeforeEach
    fun setUp() {
        searchFieldMapper = BookSearchFieldMapper()
    }

    @Test
    fun `getMapping should return correct Simple mapping for valid domain field`() {
        val titleMapping = searchFieldMapper.getMapping("title")
        assertTrue(titleMapping is FieldMapping.Simple)
        assertEquals("title", (titleMapping as FieldMapping.Simple).path)

        val authorMapping = searchFieldMapper.getMapping("authors.firstName")
        assertTrue(authorMapping is FieldMapping.Simple)
        assertEquals("authors.firstName", (authorMapping as FieldMapping.Simple).path)
    }

    @Test
    fun `getMapping should throw IllegalArgumentException for invalid domain field`() {
        assertThrows<IllegalArgumentException> {
            searchFieldMapper.getMapping("invalidField")
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
        val allowedFields = searchFieldMapper.getAllowedFields()
        assertTrue(allowedFields.contains("title"))
        assertTrue(allowedFields.contains("authors.firstName"))
    }
}