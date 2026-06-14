package io.legado.app.model.localBook

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
                importFile = { imported.add(it.name) }
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
                }
            )

            val result = importer.scanAndImport()

            assertEquals(1, result.importedCount)
            assertEquals(2, result.scannedCount)
            assertEquals(listOf("a.txt"), result.failedFiles)
        } finally {
            root.deleteRecursively()
        }
    }
}
