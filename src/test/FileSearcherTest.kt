package test

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

// Imports from JUnit 5 for tests and assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

// Standard library imports for file handling
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText

class FileSearcherTest {

    // This JUnit 5 annotation creates a temporary directory before each test
    // and automatically cleans it up afterward. This is the perfect sandbox for our tests.
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should find all occurrences in multiple files and subdirectories`() = runTest {
        // --- 1. SETUP ---
        // Create the files and directory structure programmatically.
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

        // --- 2. EXECUTION ---
        // Call the function we want to test and collect the results into a list.
        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()

        // --- 3. VERIFICATION ---
        // First, check the total count to quickly catch major errors.
        assertEquals(6, occurrences.size)

        // Create a set of expected results. We use a Set because the file search
        // is concurrent, so the order of results is not guaranteed.
        val expectedOccurrences = setOf(
            FileOccurrence(file = file1, line = 1, offset = 28),
            FileOccurrence(file = file1, line = 2, offset = 16),
            FileOccurrence(file = file1, line = 2, offset = 32),
            FileOccurrence(file = file2, line = 1, offset = 28),
            FileOccurrence(file = file4, line = 1, offset = 1),
            FileOccurrence(file = file4, line = 2, offset = 12)
        )

        // Compare the sets to verify the correctness of all found occurrences.
        assertEquals(expectedOccurrences, occurrences.toSet())
    }

    @Test
    fun `should return an empty flow when no files contain the string`() = runTest {
        // Setup a file that does not contain the search string.
        tempDir.resolve("file.txt").writeText("Hello world, this is a test.")

        // Execute the search.
        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()

        // Verify that the resulting list is empty.
        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun `should throw IllegalArgumentException for an empty search string`() {
        // This test does not need 'runTest' because the function should fail
        // immediately before launching any coroutines.

        // Verify that calling the function with an empty string throws the correct exception.
        val exception = assertThrows<IllegalArgumentException> {
            searchForTextOccurrences("", tempDir)
        }

        // Optionally, check the exception message for more detail.
        assertEquals("The string to search for cannot be empty.", exception.message)
    }

    @Test
    fun `should return an empty flow when searching in an empty directory`() = runTest {
        val occurrences = searchForTextOccurrences("Kotlin", tempDir).toList()

        assertTrue(occurrences.isEmpty())
    }
}