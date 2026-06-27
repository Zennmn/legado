package io.legado.app.help

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookProgress

object AppWebDav {
    val authorization: Nothing? = null

    fun uploadBookProgress(book: Book, toast: Boolean = false, successAction: (() -> Unit)? = null) {
        successAction?.invoke()
    }

    fun getBookProgress(book: Book, successAction: ((BookProgress) -> Unit)? = null) = Unit
}
