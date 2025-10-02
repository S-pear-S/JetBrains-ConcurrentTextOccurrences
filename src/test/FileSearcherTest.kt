package test

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText

class FileSearcherTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should find all occurrences in multiple files and subdirectories`() = runTest {
        val file1 = tempDir.resolve("file1.txt")
        file1.writeText("""
            This line contains the word Kotlin once.
            This line has Kotlin and more Kotlin.
        """.trimIndent())

        val subDir = tempDir.resolve("subdir").createDirectory()
        val file2 = subDir.resolve("file2.log")
        file2.writeText("INFO: This is a log from a Kotlin project.")

        val file3 = tempDir.resolve("no_match.txt")
        file3.writeText("This has no relevant words.")

        val file4 = tempDir.resolve("edge_cases.txt")
        file4.writeText("""
            KotlinKotlin
            Start with Kotlin, end with Kotlin
        """.trimIndent())

        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()

        assertEquals(6, occurrences.size)

        val expectedOccurrences = setOf(
            FileOccurrence(file = file1, line = 1, offset = 28),
            FileOccurrence(file = file1, line = 2, offset = 16),
            FileOccurrence(file = file1, line = 2, offset = 32),
            FileOccurrence(file = file2, line = 1, offset = 28),
            FileOccurrence(file = file4, line = 1, offset = 1),
            FileOccurrence(file = file4, line = 2, offset = 12)
        )

        assertEquals(expectedOccurrences, occurrences.toSet())
    }

    @Test
    fun `should return an empty flow when no files contain the string`() = runTest {
        tempDir.resolve("file.txt").writeText("Hello world, this is a test.")

        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()
        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun `should throw IllegalArgumentException for an empty search string`() {
        val exception = assertThrows<IllegalArgumentException> {
            searchForTextOccurrences("", tempDir)
        }

        assertEquals("The string to search for cannot be empty.", exception.message)
    }

    @Test
    fun `should return an empty flow when searching in an empty directory`() = runTest {
        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()

        assertTrue(occurrences.isEmpty())
    }
}