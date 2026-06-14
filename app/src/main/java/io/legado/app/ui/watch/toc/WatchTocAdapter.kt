package io.legado.app.ui.watch.toc

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ItemWatchTocBinding

class WatchTocAdapter(
    private val onClick: (BookChapter) -> Unit
) : RecyclerView.Adapter<WatchTocAdapter.Holder>() {

    private val chapters = arrayListOf<BookChapter>()
    var currentChapterIndex: Int = 0

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(items: List<BookChapter>) {
        chapters.clear()
        chapters.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemWatchTocBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = chapters.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(chapters[position])
    }

    inner class Holder(
        private val binding: ItemWatchTocBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chapter: BookChapter) = binding.run {
            tvTitle.text = chapter.title.ifBlank { "第 ${chapter.index + 1} 章" }
            tvIndex.text = if (chapter.index == currentChapterIndex) {
                "当前 · 第 ${chapter.index + 1} 章"
            } else {
                "第 ${chapter.index + 1} 章"
            }
            root.setOnClickListener { onClick(chapter) }
        }
    }
}
