package com.mikesajak.ebooklib.importing.domain.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportUtilsTest {

    @Test
    fun `isUnlikelyTitle identifies empty or blank titles as unlikely`() {
        assertTrue(ImportUtils.isUnlikelyTitle(null))
        assertTrue(ImportUtils.isUnlikelyTitle(""))
        assertTrue(ImportUtils.isUnlikelyTitle("   "))
    }

    @Test
    fun `isUnlikelyTitle identifies numeric or single character titles as unlikely`() {
        assertTrue(ImportUtils.isUnlikelyTitle("1"))
        assertTrue(ImportUtils.isUnlikelyTitle("01"))
        assertTrue(ImportUtils.isUnlikelyTitle("123"))
        assertTrue(ImportUtils.isUnlikelyTitle("a"))
    }

    @Test
    fun `isUnlikelyTitle identifies placeholder titles as unlikely`() {
        assertTrue(ImportUtils.isUnlikelyTitle("Untitled"))
        assertTrue(ImportUtils.isUnlikelyTitle("COVER"))
        assertTrue(ImportUtils.isUnlikelyTitle("document"))
        assertTrue(ImportUtils.isUnlikelyTitle("Unknown"))
        assertTrue(ImportUtils.isUnlikelyTitle("Page 1"))
        assertTrue(ImportUtils.isUnlikelyTitle("Chapter 1"))
        assertTrue(ImportUtils.isUnlikelyTitle("Table of Contents"))
    }

    @Test
    fun `isUnlikelyTitle returns false for valid non-placeholder titles`() {
        assertFalse(ImportUtils.isUnlikelyTitle("The Great Gatsby"))
        assertFalse(ImportUtils.isUnlikelyTitle("Clean Code"))
        assertFalse(ImportUtils.isUnlikelyTitle("Chapter 1: The Beginning"))
    }
}
