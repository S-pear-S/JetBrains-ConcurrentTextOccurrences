package searcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.readLines

fun searchForTextOccurrences(
    stringToSearch: String,
    directory: Path
): Flow<Occurrence> {

    require(stringToSearch.isNotEmpty()) { "The string to search for cannot be empty." }

    return channelFlow {
        val filesToSearch = Files.walk(directory)
            .filter { path -> path.isRegularFile() && path.isReadable() }
            .filter { path -> !path.fileName.toString().endsWith(".log")}
            .collect(Collectors.toList())

        filesToSearch.forEach { file ->
            launch {
                try {
                    val lines = file.readLines()
                    lines.forEachIndexed { index, lineText ->
                        val lineNumber = index + 1
                        var currentIndex = 0
                        while (currentIndex < lineText.length) {
                            val foundIndex = lineText.indexOf(stringToSearch, startIndex = currentIndex)
                            if (foundIndex != -1) {
                                send(FileOccurrence(file, lineNumber, foundIndex + 1))
                                currentIndex = foundIndex + stringToSearch.length
                            } else {
                                break
                            }
                        }
                    }
                } catch (e: Exception) {

                }
            }
        }
    }
        .flowOn(Dispatchers.IO)
}