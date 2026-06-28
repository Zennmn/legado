package io.legado.app.ui.welcome

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class WatchWelcomeTest {

    @Test
    fun welcomeLayoutUsesWatchCopy() {
        val xml = readFile("src/main/res/layout/activity_welcome.xml")

        assertTrue(xml.contains("阅读手表版"))
        assertTrue(xml.contains("离线 TXT 阅读器"))
        assertFalse(xml.contains("扫描 Download 中的 txt"))
    }

    @Test
    fun welcomeActivityDoesNotUseLegacyCustomWelcome() {
        val source = readFile("src/main/java/io/legado/app/ui/welcome/WelcomeActivity.kt")

        assertFalse(source.contains("customWelcome"))
        assertFalse(source.contains("welcomeImage"))
        assertFalse(source.contains("welcomeShowText"))
    }

    private fun readFile(path: String): String {
        val file = listOf(Paths.get(path), Paths.get("app", path)).first(Files::exists)
        return String(Files.readAllBytes(file))
    }
}
