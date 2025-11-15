package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

import com.mikesajak.ebooklib.book.domain.model.Book
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.criteria.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.jpa.domain.Specification

class RSQLSpecParserTest {
    private lateinit var rsqlSpecParser: RSQLSpecParser

    @BeforeEach
    fun setUp() {
        val searchFieldMapper = SearchFieldMapper() // Use real mapper for integration with parser
        rsqlSpecParser = RSQLSpecParser(searchFieldMapper)
    }

    @Test
    fun `parse should return a Specification for a valid RSQL query`() {
        // Given
        val query = "title==\"Test Book\""

        // When
        val spec: Specification<Book> = rsqlSpecParser.parse(query)

        // Then
        assertNotNull(spec)
        // Further verification of the Specification itself is complex without a real EntityManager.
        // We rely on RSQLVisitorSpecBuilder tests and integration tests for full validation.
    }

    @Test
    fun `parseOrNull should return a Specification for a valid RSQL query`() {
        // Given
        val query = "title==\"Test Book\""

        // When
        val spec: Specification<Book>? = rsqlSpecParser.parseOrNull(query)

        // Then
        assertNotNull(spec)
    }

    @Test
    fun `parseOrNull should return null for null or blank query`() {
        assertNull(rsqlSpecParser.parseOrNull<Void>(null))
        assertNull(rsqlSpecParser.parseOrNull<Void>(""))
        assertNull(rsqlSpecParser.parseOrNull<Void>("   "))
    }

    @Test
    fun `parseOrNull should throw SearchQueryException for invalid RSQL query`() {
        // Given
        val invalidQuery = "title==\"Test Book" // Missing closing quote

        // When & Then
        assertThrows<SearchQueryException> {
            rsqlSpecParser.parseOrNull<Void>(invalidQuery)
        }
    }

    @Test
    fun `parse should throw IllegalArgumentException for disallowed field`() {
        // Given
        val query = "invalidField==\"value\""

        // When & Then
        assertThrows<IllegalArgumentException> {
            rsqlSpecParser.parse<Void>(query)
        }
    }

    // --- RSQLVisitorSpecBuilder (indirect) Tests ---
    // These tests verify the behavior of RSQLVisitorSpecBuilder through RSQLSpecParser

    @Test
    fun `parse should handle AND operator correctly`() {
        // Given
        val query = "title==\"Book A\";description==\"Description A\""
        val spec: Specification<Book> = rsqlSpecParser.parse(query)

        // When (simulate predicate building)
        val cb: CriteriaBuilder = mockk()
        val root: Root<Book> = mockk()
        val cq: CriteriaQuery<*> = mockk()

        val titlePredicate: Predicate = mockk()
        val descriptionPredicate: Predicate = mockk()

        val titlePath: Path<String> = mockk()
        val descriptionPath: Path<String> = mockk()

        every { root.get<String>("title") } returns titlePath
        every { root.get<String>("description") } returns descriptionPath

        every { titlePath.javaType } returns String::class.java
        every { descriptionPath.javaType } returns String::class.java

        every { cb.equal(titlePath, "Book A") } returns titlePredicate
        every { cb.equal(descriptionPath, "Description A") } returns descriptionPredicate

        every { cb.equal(titlePath, "Book A") } returns titlePredicate
        every { cb.equal(descriptionPath, "Description A") } returns descriptionPredicate
        every {
            cb.and(titlePredicate as Expression<Boolean>,
                   descriptionPredicate as Expression<Boolean>)
        } returns mockk()

        // Then (this part is hard to test without a real JPA setup,
        // so we verify that the RSQLVisitorSpecBuilder is called and doesn't throw)
        assertDoesNotThrow {
            spec.toPredicate(root, cq, cb)
        }
    }

    @Test
    fun `parse should handle OR operator correctly`() {
        // Given
        val query = "title==\"Book A\",description==\"Description A\""
        val spec: Specification<Book> = rsqlSpecParser.parse(query)

        // When (simulate predicate building)
        val cb: CriteriaBuilder = mockk()
        val root: Root<Book> = mockk()
        val cq: CriteriaQuery<*> = mockk()

        val titlePredicate: Predicate = mockk()
        val descriptionPredicate: Predicate = mockk()

        val titlePath: Path<String> = mockk()
        val descriptionPath: Path<String> = mockk()

        every { root.get<String>("title") } returns titlePath
        every { root.get<String>("description") } returns descriptionPath

        every { titlePath.javaType } returns String::class.java
        every { descriptionPath.javaType } returns String::class.java

        every { cb.equal(titlePath, "Book A") } returns titlePredicate
        every { cb.equal(descriptionPath, "Description A") } returns descriptionPredicate
        every {
            cb.or(titlePredicate as Expression<Boolean>,
                  descriptionPredicate as Expression<Boolean>)
        } returns mockk()

        // Then
        assertDoesNotThrow {
            spec.toPredicate(root, cq, cb)
        }
    }

    @Test
    fun `parse should handle nested fields correctly`() {
        // Given
        val query = "authors.firstName==\"John\""
        val spec: Specification<Book> = rsqlSpecParser.parse(query)

        // When (simulate predicate building)
        val cb: CriteriaBuilder = mockk(relaxed = true)
        val root: Root<Book> = mockk(relaxed = true)
        val cq: CriteriaQuery<*> = mockk(relaxed = true)

        val authorJoin: Join<Any, Any> = mockk(relaxed = true)
        val firstNamePath: Path<String> = mockk(relaxed = true)
        val predicate: Predicate = mockk(relaxed = true)

        every { root.joins } returns emptySet()
        every { root.join<Any, Any>("authors", JoinType.LEFT) } returns authorJoin
        every { authorJoin.get<String>("firstName") } returns firstNamePath
        every { firstNamePath.javaType } returns String::class.java
        every { cb.equal(firstNamePath, "John") } returns predicate

        // Then
        assertDoesNotThrow {
            spec.toPredicate(root, cq, cb)
        }
    }

    @Test
    fun `parse should handle various comparison operators`() {
        // Given
        val query = "title!=\"Another\""
        val spec: Specification<Book> = rsqlSpecParser.parse(query)

        // When (simulate predicate building)
        val cb: CriteriaBuilder = mockk()
        val root: Root<Book> = mockk()
        val cq: CriteriaQuery<*> = mockk()

        val titlePath: Path<String> = mockk()
        every { root.get<String>("title") } returns titlePath
        every { titlePath.javaType } returns String::class.java

        every { cb.equal(titlePath, any<String>()) } returns mockk()
        every { cb.notEqual(titlePath, any<String>()) } returns mockk()
        every { cb.`in`(titlePath) } returns mockk<CriteriaBuilder.In<String>>()
        every { cb.lessThan(titlePath, any<String>()) } returns mockk()
        every { cb.lessThanOrEqualTo(titlePath, any<String>()) } returns mockk()
        every { cb.greaterThan(titlePath, any<String>()) } returns mockk()
        every { cb.greaterThanOrEqualTo(titlePath, any<String>()) } returns mockk()
        every { cb.and(any<Predicate>(), any<Predicate>()) } returns mockk() // For multiple predicates

        // Then
        assertDoesNotThrow {
            spec.toPredicate(root, cq, cb)
        }
    }

}