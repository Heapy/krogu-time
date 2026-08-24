# Krogu Time

Idea behind krogu time is straightforward: allow converting Java/Kotlin apps to KMP without rewriting much code. Provide JDK APIs and identical behavior to JDK, but make cleanroom implementation in Kotlin.

Rules to follow while converting API:
- Even if in Kotlin a non-null parameter is more logical, for migration purposes allow null parameter
- Replace Java-only APIs like Streams with Kotlin alternatives (Stream -> Sequence)
