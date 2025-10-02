package searcher

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class FileSearcherTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should find two occurrences in the documentation file`() = runTest {
        // Arrange
        val file = tempDir.resolve("documentation.txt")
        file.writeText("""
            Welcome to the documentation for our Kotlin project.
            The project uses Kotlin coroutines for concurrency.
        """.trimIndent())

        // Act
        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()

        // Assert
        assertEquals(2, occurrences.size)
        val expected = setOf(
            FileOccurrence(file = file, line = 1, offset = 38),
            FileOccurrence(file = file, line = 2, offset = 18)
        )
        assertEquals(expected, occurrences.toSet())
    }

    @Test
    fun `should find five occurrences with complex cases in the overview file`() = runTest {
        // Arrange
        val file = tempDir.resolve("project_overview.txt")
        file.writeText("""
            KotlinKotlin is a great language.
            Start with Kotlin, end with Kotlin
            This line has one more: Kotlin.
        """.trimIndent())

        // Act
        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()

        // Assert
        assertEquals(5, occurrences.size)
        val expected = setOf(
            FileOccurrence(file = file, line = 1, offset = 1),
            FileOccurrence(file = file, line = 1, offset = 7),
            FileOccurrence(file = file, line = 2, offset = 12),
            FileOccurrence(file = file, line = 2, offset = 29),
            FileOccurrence(file = file, line = 3, offset = 25)
        )
        assertEquals(expected, occurrences.toSet())
    }

    @Test
    fun `should ignore files that end with log`() = runTest {
        // Arrange
        val textFile = tempDir.resolve("search_here.txt")
        textFile.writeText("A match for Kotlin is in this file.")

        val logFile = tempDir.resolve("ignore_this.log")
        logFile.writeText("This Kotlin mention should be ignored by the search.")

        // Act
        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()

        // Assert
        assertEquals(1, occurrences.size)
        assertEquals(textFile, occurrences.first().file)
    }

    @Test
    fun `should return an empty flow when no files contain the search string`() = runTest {
        // Arrange
        tempDir.resolve("no_matches.txt").writeText("This file is about Java and Python.")

        // Act
        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()

        // Assert
        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun `should be case-sensitive and not find mismatched cases`() = runTest {
        tempDir.resolve("case_test.txt").writeText("This file mentions kotlin in lowercase.")

        // Act
        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()

        // Assert
        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun `should return an empty flow when searching in an empty directory`() = runTest {
        // Act
        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()

        // Assert
        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun `should throw IllegalArgumentException for an empty search string`() {
        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            searchForTextOccurrences("", tempDir)
        }

        assertEquals("The string to search for cannot be empty.", exception.message)
    }
}