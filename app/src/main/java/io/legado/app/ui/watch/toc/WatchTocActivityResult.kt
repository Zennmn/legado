package io.legado.app.ui.watch.toc

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class WatchTocActivityResult : ActivityResultContract<String, Array<Any>?>() {

    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, WatchTocActivity::class.java)
            .putExtra("bookUrl", input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Array<Any>? {
        if (resultCode != RESULT_OK || intent == null) {
            return null
        }
        return arrayOf(
            intent.getIntExtra("index", 0),
            intent.getIntExtra("chapterPos", 0),
            intent.getBooleanExtra("chapterChanged", false),
            0,
            0
        )
    }
}
