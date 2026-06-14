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

    @Test
    fun listTxtFilesScansSubdirectories() {
        val root = createTempDirectory(prefix = "watch-txt-filter-sub-").toFile()
        try {
            File(root, "top.txt").writeText("top")
            val sub = File(root, "小说")
            sub.mkdir()
            File(sub, "三体.txt").writeText("三体")
            File(sub, "cover.jpg").writeText("jpg")
            val sub2 = File(sub, "系列")
            sub2.mkdir()
            File(sub2, "黑暗森林.txt").writeText("黑暗森林")

            val names = WatchTxtFileFilter.listTxtFiles(root).map { it.name }

            assertEquals(listOf("top.txt", "三体.txt", "黑暗森林.txt"), names)
        } finally {
            root.deleteRecursively()
        }
    }
}
