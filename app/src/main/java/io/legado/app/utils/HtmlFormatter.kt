package io.legado.app.utils

@Suppress("RegExpRedundantEscape")
object HtmlFormatter {
    private val nbspRegex = "(&nbsp;)+".toRegex()
    private val espRegex = "(&ensp;|&emsp;)".toRegex()
    private val noPrintRegex = "(&thinsp;|&zwnj;|&zwj;|\u2009|\u200C|\u200D)".toRegex()
    private val wrapHtmlRegex = "</?(?:div|p|br|hr|h\\d|article|dd|dl)[^>]*>".toRegex()
    private val commentRegex = "<!--[^>]*-->".toRegex()
    private val otherHtmlRegex = "</?[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val notImgHtmlRegex = "</?(?!img)[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val indent1Regex = "\\s*\\n+\\s*".toRegex()
    private val indent2Regex = "^[\\n\\s]+".toRegex()
    private val lastRegex = "[\\n\\s]+$".toRegex()

    fun format(html: String?, otherRegex: Regex = otherHtmlRegex): String {
        html ?: return ""
        return html.replace(nbspRegex, " ")
            .replace(espRegex, " ")
            .replace(noPrintRegex, "")
            .replace(wrapHtmlRegex, "\n")
            .replace(commentRegex, "")
            .replace(otherRegex, "")
            .replace(indent1Regex, "\n　　")
            .replace(indent2Regex, "　　")
            .replace(lastRegex, "")
    }

    fun formatKeepImg(html: String?, redirectUrl: java.net.URL? = null): String {
        html ?: return ""
        return format(html, notImgHtmlRegex)
    }
}
