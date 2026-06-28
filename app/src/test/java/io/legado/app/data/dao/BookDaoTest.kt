package io.legado.app.data.dao

import io.legado.app.constant.BookType
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BookDaoTest {

    @Test
    fun flowLocalUsesLocalBookTypeFlag() {
        val source = listOf(
            File("src/main/java/io/legado/app/data/dao/BookDao.kt"),
            File("app/src/main/java/io/legado/app/data/dao/BookDao.kt")
        ).firstOrNull { it.isFile }?.readText() ?: error("BookDao.kt not found")

        assertTrue(
            "flowLocal must filter BookType.local (${BookType.local}), not another type flag",
            source.contains("type & \${BookType.local} > 0") ||
                source.contains("type & ${BookType.local} > 0")
        )
    }
}
