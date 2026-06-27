package io.legado.app.model.localBook

import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WatchTxtImporterTest {

    @Test
    fun scanAndImportImportsOnlyTxtFiles() {
        val root = Files.createTempDirectory("watch-txt-import-").toFile()
        val imported = arrayListOf<String>()
        try {
            File(root, "b.txt").writeText("b")
            File(root, "a.TXT").writeText("a")
            File(root, "cover.jpg").writeText("jpg")

            val importer = WatchTxtImporter(
                downloadDirProvider = { root },
                importFile = { imported.add(it.name) },
                shelfBooksProvider = { emptyList() }
            )

            val result = importer.scanAndImport()

            assertEquals(listOf("a.TXT", "b.txt"), imported)
            assertEquals(2, result.importedCount)
            assertEquals(2, result.scannedCount)
            assertEquals(emptyList<String>(), result.failedFiles)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun scanAndImportRecordsFailuresAndContinues() {
        val root = Files.createTempDirectory("watch-txt-import-fail-").toFile()
        try {
            File(root, "a.txt").writeText("a")
            File(root, "b.txt").writeText("b")

            val importer = WatchTxtImporter(
                downloadDirProvider = { root },
                importFile = {
                    if (it.name == "a.txt") error("boom")
                },
                shelfBooksProvider = { emptyList() }
            )

            val result = importer.scanAndImport()

            assertEquals(1, result.importedCount)
            assertEquals(2, result.scannedCount)
            assertEquals(listOf("a.txt"), result.failedFiles)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun scanAndImportRemovesMissingDownloadTxtBooksFromShelf() {
        val root = Files.createTempDirectory("watch-txt-import-prune-").toFile()
        val removed = arrayListOf<String>()
        try {
            File(root, "exists.txt").writeText("exists")
            val missing = File(root, "missing.txt")

            val importer = WatchTxtImporter(
                downloadDirProvider = { root },
                importFile = {},
                shelfBooksProvider = {
                    listOf(
                        localTxtBook(File(root, "exists.txt")),
                        localTxtBook(missing)
                    )
                },
                removeBookFromShelf = { removed.add(it.originName) }
            )

            val result = importer.scanAndImport()

            assertEquals(listOf("missing.txt"), removed)
            assertEquals(1, result.removedCount)
            assertEquals(1, result.importedCount)
            assertEquals(1, result.scannedCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun scanAndImportKeepsNonDownloadAndNonTxtBooksWhenPruning() {
        val root = Files.createTempDirectory("watch-txt-import-keep-").toFile()
        val otherRoot = Files.createTempDirectory("watch-txt-import-other-").toFile()
        val removed = arrayListOf<String>()
        try {
            val outsideMissingTxt = File(otherRoot, "outside.txt")
            val missingPdf = File(root, "missing.pdf")

            val importer = WatchTxtImporter(
                downloadDirProvider = { root },
                importFile = {},
                shelfBooksProvider = {
                    listOf(
                        localTxtBook(outsideMissingTxt),
                        localBook(missingPdf, BookType.local)
                    )
                },
                removeBookFromShelf = { removed.add(it.originName) }
            )

            val result = importer.scanAndImport()

            assertEquals(emptyList<String>(), removed)
            assertEquals(0, result.removedCount)
            assertEquals(0, result.importedCount)
            assertEquals(0, result.scannedCount)
        } finally {
            root.deleteRecursively()
            otherRoot.deleteRecursively()
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
