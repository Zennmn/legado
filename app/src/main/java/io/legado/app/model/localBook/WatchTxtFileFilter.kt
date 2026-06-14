package io.legado.app.model.localBook

import java.io.File
import java.util.Locale

object WatchTxtFileFilter {

    fun isTxtFileName(name: String): Boolean {
        if (name.startsWith(".")) return false
        return name.lowercase(Locale.ROOT).endsWith(".txt")
    }

    fun listTxtFiles(root: File): List<File> {
        if (!root.exists() || !root.isDirectory) return emptyList()
        return root.walk()
            .filter { it.isFile }
            .filter { isTxtFileName(it.name) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            .toList()
    }
}
