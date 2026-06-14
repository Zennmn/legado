package io.legado.app.model.localBook

import android.net.Uri
import android.os.Environment
import java.io.File

data class WatchTxtImportResult(
    val scannedCount: Int,
    val importedCount: Int,
    val failedFiles: List<String>
)

class WatchTxtImporter(
    private val downloadDirProvider: () -> File = {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    },
    private val importFile: (File) -> Unit = { file ->
        LocalBook.importFile(Uri.fromFile(file))
    }
) {

    fun scanAndImport(): WatchTxtImportResult {
        val files = WatchTxtFileFilter.listTxtFiles(downloadDirProvider())
        var importedCount = 0
        val failedFiles = arrayListOf<String>()
        files.forEach { file ->
            runCatching {
                importFile(file)
            }.onSuccess {
                importedCount += 1
            }.onFailure {
                failedFiles.add(file.name)
            }
        }
        return WatchTxtImportResult(
            scannedCount = files.size,
            importedCount = importedCount,
            failedFiles = failedFiles
        )
    }
}
