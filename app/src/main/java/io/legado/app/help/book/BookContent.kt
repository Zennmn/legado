package io.legado.app.help.book

data class BookContent(
    val sameTitleRemoved: Boolean,
    val textList: List<String>,
    //起效的替换规则
    val effectiveReplaceRules: List<*>?
) {

    override fun toString(): String {
        return textList.joinToString("\n")
    }

}
