package io.legado.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.legado.app.data.entities.Book
import java.io.File

object ImageUtils {

    fun decode(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int,
        source: Any? = null,
        book: Book? = null
    ): Bitmap {
        return bitmap
    }

    fun decode(
        file: File,
        maxWidth: Int,
        maxHeight: Int,
        source: Any? = null,
        book: Book? = null
    ): Bitmap? {
        return BitmapUtils.decodeBitmap(file.absolutePath, maxWidth, maxHeight)
    }

    fun skipDecode(source: Any?, isCover: Boolean): Boolean = false

    fun decodeToSize(
        source: Any?,
        url: String,
        isCover: Boolean
    ): Pair<Int, Int>? {
        return null
    }
}
