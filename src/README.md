# Concurrent Text Occurrence Finder in Kotlin
This project is a command-line tool that searches for text occurrences within a specified directory. It is written in Kotlin and leverages modern programming practices, with a strong focus on concurrency, robustness, and testability.

This project was completed as part of an exercise for a JetBrains internship application.

---

## How to Build and Run

### Prerequisites

**Java Development Kit (JDK)**: Version 11 or higher is required.
**Gradle**: No installation is needed. The project includes a Gradle wrapper (`./gradlew`) which will automatically download the correct version.

### Build the Project

This command will compile all the Kotlin source code, run the unit tests, and assemble the application. Run the following command from the root directory of the project:

```bash
./gradlew build
```

### Run the Application

This command executes the main entry point of the project. The application will start in your terminal and prompt you for input.

```bash
./gradlew run
```

### Run the Tests

This command will execute the complete suite of unit tests located in the src/test/kotlin directory. The results will be displayed in the terminal.

```bash
./gradlew test
```

---

## Architectural Decisions and Key Features

### 1. Concurrency Model: Kotlin Coroutines and `channelFlow`

The primary performance bottleneck in a file search application is **I/O (Input/Output)**—the time spent waiting for the operating system to read files from the disk. A sequential search that processes one file at a time would be incredibly inefficient, as the CPU would sit idle for the majority of the time.

To solve this, the application was designed to be concurrent from the ground up using **Kotlin Coroutines**.

**Why Coroutines?**: Coroutines are the idiomatic, modern solution for asynchronous and concurrent programming in Kotlin. They are extremely lightweight compared to traditional threads, meaning we can launch thousands of them without significant overhead. This allows us to assign a separate coroutine to each file in the directory, enabling massive parallelism. The use of `structured concurrency` also ensures that all launched coroutines are properly managed and no work is leaked.
**Why `channelFlow` Was a Critical Choice**: While coroutines provide the mechanism for parallelism, we needed a way to safely collect the results from all these concurrent "worker" coroutines into a single, ordered stream (`Flow`). A standard `flow { ... }` builder is not designed for this; it is not thread-safe for emissions and expects a single, sequential producer. Our design has multiple producers (one coroutine per file) trying to emit results at the same time.
**`channelFlow`** was chosen specifically because it is the purpose-built tool for this exact scenario. It uses a **thread-safe Channel** under the scenes, which acts as a robust queue. This allows many different coroutines to `send` (emit) their results concurrently without causing race conditions or the `IllegalStateException` that would occur with a standard `flow`. This choice ensures our concurrent model is not only fast but also safe and correct.

### 2. Architecture: Strict Separation of Concerns (SoC)

The project's architecture is built on the principle of Separation of Concerns, ensuring that each part of the codebase has a single, well-defined responsibility. This makes the system significantly more maintainable, testable, and reusable.

The components are divided as follows:

**Application Entry Point (`Main.kt`)**: Responsible only for handling user interaction and orchestrating the application flow. It contains no core search logic itself.
**Core Business Logic (`FileSearcher.kt`)**: A pure, stateless function that accepts inputs and produces outputs. It is completely decoupled from the UI, logging, or any other external service.
**Data Model (`Occurrence.kt`)**: Simple `data class` and `interface` that only define the structure of our results, holding no logic.
**Service (`Logger.kt`)**: A centralized singleton responsible for all logging operations.

**Why This Matters:** This separation makes the core logic in `FileSearcher.kt` a **highly reusable library**. It can be tested in complete isolation without needing to simulate console input, and it could be easily repurposed for a different front-end (like a GUI or web service) without any changes.

### 3. Code Safety: Immutability and Robust Error Handling

Writing correct concurrent code requires a strong focus on safety. Two key principles were followed to ensure the application is robust and predictable.

**Safety Through Immutability**: The `FileOccurrence` data class, which represents a single search result, is defined using immutable `val` properties. This is a critical design choice in concurrent programming. Once an `Occurrence` object is created by one coroutine, it is guaranteed to be unchangeable. This makes it completely safe to pass these objects between different coroutines without the risk of data corruption or race conditions, eliminating the need for complex synchronization mechanisms like locks.
**Robust Error Handling**: File system operations are inherently risky—a file might be unreadable, corrupted, or access could be denied. The file processing logic for each coroutine is wrapped in a `try-catch` block. This ensures that an exception thrown while reading one file will **not crash the entire application**. The failing coroutine will terminate gracefully, and the others will continue their work, making the tool resilient and able to complete its search even when encountering problematic files.

### 4. Testing Strategy: Isolation and Concurrency-Safe Assertions

A comprehensive and reliable test suite was a core goal of this project to guarantee the correctness of the search logic. The testing strategy was designed to address two primary challenges: ensuring tests are independent and correctly handling the output of a concurrent system.

**Test Isolation (`@TempDir`)**: A fundamental principle of unit testing is that tests should be completely independent and should not interfere with each other. To achieve this, every test function in the suite is annotated with JUnit 5's `@TempDir`. This powerful feature automatically creates a fresh, empty directory on the file system before each test and guarantees its deletion after the test completes. This ensures that every test runs in a perfectly clean, isolated "sandbox," making them 100% repeatable and preventing any side effects.
**Concurrency-Safe Assertions (`toSet()`)**: The most significant challenge when testing a concurrent system is that the order of results is **non-deterministic**. One coroutine might finish before another, so the order in which occurrences are emitted can vary between test runs. A naive test that compares the result to a `List` would be "flaky"—it might pass sometimes and fail others. To solve this, the results are collected into a list and then converted to a `Set` before the assertion (`occurrences.toSet()`). A `Set`'s equality is based only on its contents, not the order. This makes the assertion **robust and reliable**, correctly verifying that all the expected occurrences were found, regardless of the order in which they were emitted by the concurrent workers.

---

## Future Improvements

The current implementation provides a solid and robust foundation. However, if this were to be developed into a full-featured command-line utility, the following features would be logical next steps:

### 1. Enhanced Search Capabilities

**Case-Insensitive Search**: Add an option for the user to perform a case-insensitive search. This could be implemented with an optional boolean flag passed to the `searchForTextOccurrences` function, which would change the comparison logic.
**Regular Expression (Regex) Support**: Allow the user to search for a pattern instead of a fixed string. This would significantly increase the tool's power, allowing for much more complex and flexible searches.

### 2. Configurable File Filtering

**Dynamic Include/Exclude Rules**: The current logic hardcodes the exclusion of `.log` files. A more advanced version would allow the user to specify which file extensions to search or ignore via command-line arguments (e.g., `--include .kt,.java` or `--exclude .xml`).

### 3. Concurrency Throttling

**Limited Parallelism**: The current model launches a new coroutine for every file. If a directory contains tens of thousands of small files, this could create a very high number of active coroutines. A more advanced implementation would use a `Semaphore` or a fixed-size dispatcher to limit the number of files being processed concurrently (e.g., to the number of CPU cores), preventing system overload.

### 4. Advanced Output Formatting

**Contextual Results**: Add an option to show context around a found occurrence (e.g., the line before and after the match, similar to the `grep -C` command). This often provides more useful results than just the matching line itself.
**Structured Output (JSON)**: Provide an option to format the output as JSON. This would make the tool composable, allowing its output to be easily piped into other scripts or programs for further processing.