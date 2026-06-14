package io.legado.app.ui.watch

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityWatchBookshelfBinding
import io.legado.app.utils.gone
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WatchBookshelfActivity :
    VMBaseActivity<ActivityWatchBookshelfBinding, WatchBookshelfViewModel>(
        theme = Theme.Dark,
        imageBg = false
    ) {

    override val binding by viewBinding(ActivityWatchBookshelfBinding::inflate)
    override val viewModel by viewModels<WatchBookshelfViewModel>()

    private val adapter by lazy {
        WatchBookAdapter { book ->
            startActivityForBook(book)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        WatchReaderDefaults.apply()
        initView()
        observeBooks()
        viewModel.scanDownload()
    }

    private fun initView() = binding.run {
        swipeBackLayout.setOnEdgeBack { finish() }
        recyclerView.layoutManager = LinearLayoutManager(this@WatchBookshelfActivity)
        recyclerView.adapter = adapter
        tvScan.setOnClickListener {
            viewModel.scanDownload()
        }
    }

    private fun observeBooks() {
        lifecycleScope.launch {
            viewModel.booksFlow
                .catch {
                    binding.tvEmpty.visible()
                    binding.recyclerView.gone()
                }
                .collectLatest { books ->
                    adapter.submitList(books)
                    binding.recyclerView.visible(books.isNotEmpty())
                    binding.tvEmpty.visible(books.isEmpty())
                }
        }
    }
}
