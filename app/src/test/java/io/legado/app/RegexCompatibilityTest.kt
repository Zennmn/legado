package io.legado.app

import io.legado.app.constant.AppPattern
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class RegexCompatibilityTest {

    @Test
    fun appPatternAvoidsAndroidIncompatibleShortPunctuationClass() {
        assertFalse(AppPattern.bdRegex.pattern.contains("\\p{P}"))
        assertFalse(AppPattern.notReadAloudRegex.pattern.contains("\\p{P}"))
    }

    @Test
    fun defaultTxtTocRulesAvoidVariableLengthLookBehind() {
        val path = listOf(
            Paths.get("src/main/assets/defaultData/txtTocRule.json"),
            Paths.get("app/src/main/assets/defaultData/txtTocRule.json")
        ).first(Files::exists)
        val json = String(Files.readAllBytes(path))

        assertFalse(json.contains("(?<=[\\\\s　]{0,4})"))
        assertFalse(json.contains("(?<=[ 　\\\\t]{0,4})"))
        assertFalse(json.contains("(?m)(?<=[ \\\\t　]{0,4}"))
    }

    @Test
    fun textChapterLayoutAvoidsIcuIncompatibleBraceEscapePattern() {
        val path = listOf(
            Paths.get("src/main/java/io/legado/app/ui/book/read/page/provider/TextChapterLayout.kt"),
            Paths.get("app/src/main/java/io/legado/app/ui/book/read/page/provider/TextChapterLayout.kt")
        ).first(Files::exists)
        val source = String(Files.readAllBytes(path))

        assertFalse(source.contains("""Pattern.compile(",\\{.*}$")"""))
    }
}
