package searcher

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path

fun main() = runBlocking {
    SimpleLogger.log("Application started.")
    println("--- Concurrent File Searcher ---")

    print("Enter the string to search for: ")
    val stringToSearch = readlnOrNull()
    if (stringToSearch.isNullOrEmpty()) {
        val errorMsg = "The search string cannot be empty."
        println("ERROR: $errorMsg")
        SimpleLogger.log("ERROR: $errorMsg. Application shutting down.")
        return@runBlocking
    }

    print("Enter the name of the directory to search in (e.g. search_demo_files): ")
    val directoryName = readlnOrNull()
    if (directoryName.isNullOrEmpty()) {
        val errorMsg = "The directory name cannot be empty."
        println("ERROR: $errorMsg")
        SimpleLogger.log("ERROR: $errorMsg. Application shutting down.")
        return@runBlocking
    }

    val searchDirectory: Path
    try {
        val resourceUrl = {}.javaClass.classLoader.getResource(directoryName)

        if (resourceUrl == null) {
            val errorMsg = "The directory '$directoryName' was not found inside 'src/main/resources/'."
            println("\nERROR: $errorMsg")
            SimpleLogger.log("ERROR: $errorMsg")
            return@runBlocking
        }
        searchDirectory = Path.of(resourceUrl.toURI())

    } catch (e: Exception) {
        val errorMsg = "Could not create a valid path from '$directoryName'. Reason: ${e.message}"
        println("ERROR: $errorMsg")
        SimpleLogger.log("ERROR: $errorMsg")
        return@runBlocking
    }

    SimpleLogger.log("Starting search for '$stringToSearch' in '${searchDirectory.toAbsolutePath()}'.")
    println("\nSearching for '$stringToSearch'...\n")

    val occurrencesFlow = searchForTextOccurrences(stringToSearch, searchDirectory)
    var count = 0

    occurrencesFlow.collect { occurrence ->
        count++
        println(
            "Match $count: -> File='${occurrence.file.fileName}', " +
                    "Line=${occurrence.line}, " +
                    "Offset=${occurrence.offset}"
        )
    }
    println("\nSearch finished. Found a total of $count occurrences.")
    SimpleLogger.log("Search finished. Found a total of $count occurrences.")
    SimpleLogger.log("Application shutting down.")
}