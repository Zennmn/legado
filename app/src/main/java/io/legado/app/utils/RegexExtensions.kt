package io.legado.app.utils

import io.legado.app.data.entities.BookChapter
import io.legado.app.exception.RegexTimeoutException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import splitties.init.appCtx

@OptIn(ExperimentalCoroutinesApi::class)
fun CharSequence.replace(
    name: String,
    regex: Regex,
    replacement: String,
    timeout: Long,
    chapter: BookChapter? = null,
    book: Any? = null
): String {
    val charSequence = this@replace
    return runBlocking {
        try {
            withTimeout(timeout) {
                val pattern = regex.toPattern()
                val matcher = pattern.matcher(charSequence)
                val stringBuffer = StringBuffer()
                while (matcher.find()) {
                    matcher.appendReplacement(stringBuffer, replacement)
                }
                matcher.appendTail(stringBuffer)
                stringBuffer.toString()
            }
        } catch (e: Exception) {
            val timeoutMsg = "替换超时\n规则名称:$name\n替换规则:$regex"
            appCtx.longToastOnUi(timeoutMsg)
            throw RegexTimeoutException(timeoutMsg)
        }
    }
}
