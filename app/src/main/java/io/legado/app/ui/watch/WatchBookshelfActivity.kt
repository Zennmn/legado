package io.legado.app.ui.watch

import android.os.Bundle
import android.os.Environment
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityWatchBookshelfBinding
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.utils.gone
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.ui.watch.about.WatchAboutActivity
import io.legado.app.utils.startActivity
import io.legado.app.utils.visible
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class WatchBookshelfActivity :
    VMBaseActivity<ActivityWatchBookshelfBinding, WatchBookshelfViewModel>(
        theme = Theme.Dark,
        imageBg = false
    ) {

    override val binding by viewBinding(ActivityWatchBookshelfBinding::inflate)
    override val viewModel by viewModels<WatchBookshelfViewModel>()
    private val downloadDir: File by lazy {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }

    private val adapter by lazy {
        WatchBookAdapter(
            onBookClick = { book ->
                if (viewModel.isMissingDownloadTxtBook(book, downloadDir)) {
                    viewModel.deleteFromShelf(book, "文件不存在，已从书架移除")
                } else {
                    startActivityForBook(book)
                }
            },
            onBookLongClick = { book ->
                viewModel.deleteFromShelf(book)
            }
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        WatchReaderDefaults.apply()
        initView()
        observeBooks()
        requestStorageAndScan()
    }

    private fun requestStorageAndScan() {
        PermissionsCompat.Builder()
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                viewModel.scanDownload()
            }
            .onDenied {
                toastOnUi("需要存储权限才能扫描 Download 目录")
            }
            .request()
    }

    private fun initView() = binding.run {
        swipeBackLayout.setOnEdgeBack { finish() }
        recyclerView.layoutManager = LinearLayoutManager(this@WatchBookshelfActivity)
        recyclerView.adapter = adapter
        tvScan.setOnClickListener {
            requestStorageAndScan()
        }
        tvAbout.setOnClickListener {
            startActivity<WatchAboutActivity>()
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
