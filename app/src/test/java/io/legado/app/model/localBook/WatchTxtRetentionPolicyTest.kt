package io.legado.app.model.localBook

import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WatchTxtRetentionPolicyTest {

    @Test
    fun keepsExistingVisibleTxtInsideDownload() {
        val root = Files.createTempDirectory("watch-retain-").toFile()
        try {
            val file = File(root, "book.txt").apply { writeText("hello") }
            val book = localTxtBook(file)

            assertTrue(WatchTxtRetentionPolicy.shouldKeep(root, book))
            assertEquals(file.canonicalPath, WatchTxtRetentionPolicy.retainedPath(root, book))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsMissingTxtInsideDownload() {
        val root = Files.createTempDirectory("watch-retain-missing-").toFile()
        try {
            val missing = File(root, "missing.txt")

            assertFalse(WatchTxtRetentionPolicy.shouldKeep(root, localTxtBook(missing)))
            assertEquals(missing.canonicalPath, WatchTxtRetentionPolicy.retainedPath(root, localTxtBook(missing)))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsTxtOutsideDownload() {
        val root = Files.createTempDirectory("watch-retain-root-").toFile()
        val outside = Files.createTempDirectory("watch-retain-outside-").toFile()
        try {
            val file = File(outside, "outside.txt").apply { writeText("outside") }

            assertFalse(WatchTxtRetentionPolicy.shouldKeep(root, localTxtBook(file)))
            assertNull(WatchTxtRetentionPolicy.retainedPath(root, localTxtBook(file)))
        } finally {
            root.deleteRecursively()
            outside.deleteRecursively()
        }
    }

    @Test
    fun rejectsNonTxtInsideDownload() {
        val root = Files.createTempDirectory("watch-retain-non-txt-").toFile()
        try {
            val file = File(root, "book.epub").apply { writeText("epub") }

            assertFalse(WatchTxtRetentionPolicy.shouldKeep(root, localBook(file, BookType.local)))
            assertNull(WatchTxtRetentionPolicy.retainedPath(root, localBook(file, BookType.local)))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsContentUriLegacyBooks() {
        val root = Files.createTempDirectory("watch-retain-content-").toFile()
        try {
            val book = Book(
                bookUrl = "content://downloads/book.txt",
                originName = "book.txt",
                name = "book",
                type = BookType.local or BookType.text
            )

            assertFalse(WatchTxtRetentionPolicy.shouldKeep(root, book))
            assertNull(WatchTxtRetentionPolicy.retainedPath(root, book))
        } finally {
            root.deleteRecursively()
        }
    }

    private fun localTxtBook(file: File): Book {
        return localBook(file, BookType.local or BookType.text)
    }

    private fun localBook(file: File, type: Int): Book {
        return Book(
            bookUrl = file.absolutePath,
            originName = file.name,
            name = file.nameWithoutExtension,
            type = type
        )
    }
}
