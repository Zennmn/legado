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
        binding.tvVersion.text = BuildConfig.VERSION_NAME
        binding.tvBody.text = "离线 TXT 阅读器。仅扫描 Download 文件夹下的 txt 文件，不提供书源、RSS、Web 服务、云同步或在线下载。"
        binding.root.setOnClickListener { finish() }
    }
}
