package io.legado.app.model.localBook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class WatchTxtFileFilterTest {

    @Test
    fun acceptsOnlyVisibleTxtNames() {
        assertTrue(WatchTxtFileFilter.isTxtFileName("book.txt"))
        assertTrue(WatchTxtFileFilter.isTxtFileName("BOOK.TXT"))
        assertTrue(WatchTxtFileFilter.isTxtFileName("小说.Txt"))

        assertFalse(WatchTxtFileFilter.isTxtFileName(".hidden.txt"))
        assertFalse(WatchTxtFileFilter.isTxtFileName("book.epub"))
        assertFalse(WatchTxtFileFilter.isTxtFileName("book.txt.bak"))
        assertFalse(WatchTxtFileFilter.isTxtFileName("folder"))
    }

    @Test
    fun listTxtFilesReturnsSortedFilesOnly() {
        val root = createTempDirectory(prefix = "watch-txt-filter-").toFile()
        try {
            File(root, "b.txt").writeText("b")
            File(root, "a.TXT").writeText("a")
            File(root, "c.epub").writeText("c")
            File(root, ".hidden.txt").writeText("hidden")
            File(root, "folder.txt").mkdir()

            val names = WatchTxtFileFilter.listTxtFiles(root).map { it.name }

            assertEquals(listOf("a.TXT", "b.txt"), names)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun missingDirectoryReturnsEmptyList() {
        val root = File(createTempDirectory(prefix = "watch-txt-filter-missing-").toFile(), "missing")

        assertEquals(emptyList<File>(), WatchTxtFileFilter.listTxtFiles(root))
    }
}
