package io.legado.app.ui.watch.about

import android.os.Bundle
import io.legado.app.BuildConfig
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityWatchAboutBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding

class WatchAboutActivity : BaseActivity<ActivityWatchAboutBinding>() {

    override val binding by viewBinding(ActivityWatchAboutBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.tvTitle.text = "阅读手表版"
        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME}"
        binding.tvBody.text = "专为圆屏手表设计的离线 TXT 阅读器。\n\n把 txt 放进 Download 文件夹即可自动导入。\n\n支持翻页、目录、字号和亮度调节，完全离线。"
        binding.root.setOnClickListener { finish() }
    }
}
