package io.legado.app.model

import android.content.Context

object ReadAloud {
    fun upReadAloudClass() = Unit
    fun play(context: Context, play: Boolean = true, startPos: Int = 0) = Unit
    fun stop(context: Context) = Unit
    fun pause(context: Context) = Unit
    fun resume(context: Context) = Unit
    fun prevParagraph(context: Context) = Unit
    fun nextParagraph(context: Context) = Unit
}
