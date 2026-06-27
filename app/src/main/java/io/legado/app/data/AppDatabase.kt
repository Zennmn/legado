package io.legado.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.legado.app.data.dao.BookChapterDao
import io.legado.app.data.dao.BookDao
import io.legado.app.data.dao.BookGroupDao
import io.legado.app.data.dao.BookmarkDao
import io.legado.app.data.dao.KeyboardAssistsDao
import io.legado.app.data.dao.ReadRecordDao
import io.legado.app.data.dao.TxtTocRuleDao
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.Bookmark
import io.legado.app.data.entities.KeyboardAssist
import io.legado.app.data.entities.ReadRecord
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.help.DefaultData
import org.intellij.lang.annotations.Language
import splitties.init.appCtx
import java.util.Locale

val appDb by lazy {
    Room.databaseBuilder(appCtx, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
        .fallbackToDestructiveMigration()
        .allowMainThreadQueries()
        .addCallback(AppDatabase.dbCallback)
        .build()
}

@Database(
    version = 1,
    exportSchema = false,
    entities = [Book::class, BookGroup::class, BookChapter::class,
        Bookmark::class, TxtTocRule::class, ReadRecord::class, KeyboardAssist::class]
)
abstract class AppDatabase : RoomDatabase() {

    abstract val bookDao: BookDao
    abstract val bookGroupDao: BookGroupDao
    abstract val bookChapterDao: BookChapterDao
    abstract val bookmarkDao: BookmarkDao
    abstract val txtTocRuleDao: TxtTocRuleDao
    abstract val readRecordDao: ReadRecordDao
    abstract val keyboardAssistsDao: KeyboardAssistsDao

    companion object {

        const val DATABASE_NAME = "legado.db"

        val dbCallback = object : Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        db.setLocale(Locale.CHINESE)
                    } catch (_: Exception) { }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                @Language("sql")
                val insertBookGroupAllSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdAll}, '全部', -10, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdAll})
                """.trimIndent()
                db.execSQL(insertBookGroupAllSql)
                @Language("sql")
                val insertBookGroupLocalSql = """
                    insert into book_groups(groupId, groupName, 'order', enableRefresh, show) 
                    select ${BookGroup.IdLocal}, '本地', -9, 0, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdLocal})
                """.trimIndent()
                db.execSQL(insertBookGroupLocalSql)
                db.query("select * from keyboardAssists order by serialNo").use {
                    if (it.count == 0) {
                        DefaultData.keyboardAssists.forEach { keyboardAssist ->
                            val contentValues = ContentValues().apply {
                                put("type", keyboardAssist.type)
                                put("key", keyboardAssist.key)
                                put("value", keyboardAssist.value)
                                put("serialNo", keyboardAssist.serialNo)
                            }
                            db.insert(
                                "keyboardAssists",
                                SQLiteDatabase.CONFLICT_REPLACE,
                                contentValues
                            )
                        }
                    }
                }
            }
        }
    }
}