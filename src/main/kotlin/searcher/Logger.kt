package searcher

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SimpleLogger {

    private val logFilePath: Path = Path.of("src/main/resources/logs/server.log")
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    init {
        try {
            Files.createDirectories(logFilePath.parent)
        } catch (e: Exception) {
            System.err.println("CRITICAL ERROR: Could not create log directory. Reason: ${e.message}")
        }
    }

    fun log(message: String) {
        try {
            val timestamp = LocalDateTime.now().format(formatter)
            val logEntry = "[INFO] [$timestamp] $message\n"

            Files.writeString(logFilePath, logEntry, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
        } catch (e: Exception) {
            System.err.println("Logging failed: ${e.message}")
        }
    }
}