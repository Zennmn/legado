package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books order by durChapterTime desc")
    fun flowAll(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE type & 4 > 0")
    fun flowLocal(): Flow<List<Book>>

    @get:Query("SELECT * FROM books")
    val all: List<Book>

    @Query("SELECT * FROM books WHERE bookUrl = :bookUrl")
    fun getBook(bookUrl: String): Book?

    @Query("SELECT * FROM books WHERE name = :name AND author = :author LIMIT 1")
    fun getBook(name: String, author: String): Book?

    @Query("SELECT EXISTS(SELECT 1 FROM books WHERE bookUrl = :bookUrl)")
    fun has(bookUrl: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM books WHERE originName = :fileName)")
    fun hasFile(fileName: String): Boolean

    @get:Query("SELECT * FROM books WHERE type & 16 > 0 ORDER BY durChapterTime DESC limit 1")
    val lastReadBook: Book?

    @get:Query("select min(`order`) from books")
    val minOrder: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg book: Book)

    @Update
    fun update(vararg book: Book)

    @Delete
    fun delete(vararg book: Book)

    @Transaction
    fun replace(oldBook: Book, newBook: Book) {
        delete(oldBook)
        insert(newBook)
    }

    @Query("update books set durChapterPos = :pos where bookUrl = :bookUrl")
    fun upProgress(bookUrl: String, pos: Int)
}
