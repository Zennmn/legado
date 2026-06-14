package io.legado.app.ui.watch.toc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ActivityWatchTocBinding
import io.legado.app.utils.gone
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible

class WatchTocActivity :
    VMBaseActivity<ActivityWatchTocBinding, WatchTocViewModel>(
        theme = Theme.Dark,
        imageBg = false
    ) {

    override val binding by viewBinding(ActivityWatchTocBinding::inflate)
    override val viewModel by viewModels<WatchTocViewModel>()

    private val adapter by lazy {
        WatchTocAdapter(::selectChapter)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        observe()
        viewModel.load(intent.getStringExtra("bookUrl").orEmpty())
    }

    private fun initView() = binding.run {
        swipeBackLayout.setOnEdgeBack { finish() }
        recyclerView.layoutManager = LinearLayoutManager(this@WatchTocActivity)
        recyclerView.adapter = adapter
    }

    private fun observe() {
        viewModel.tocLiveData.observe(this) { state ->
            adapter.currentChapterIndex = state.book.durChapterIndex
            adapter.submitList(state.chapters)
            binding.recyclerView.visible(state.chapters.isNotEmpty())
            binding.tvEmpty.visible(state.chapters.isEmpty())
            val currentPosition = state.chapters.indexOfFirst { it.index >= state.book.durChapterIndex }
            if (currentPosition >= 0) {
                binding.recyclerView.scrollToPosition(currentPosition)
            }
        }
        viewModel.errorLiveData.observe(this) {
            binding.recyclerView.gone()
            binding.tvEmpty.visible()
            toastOnUi(it)
        }
    }

    private fun selectChapter(chapter: BookChapter) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra("index", chapter.index)
                .putExtra("chapterPos", 0)
                .putExtra("chapterChanged", true)
        )
        finish()
    }
}
