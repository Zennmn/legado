package io.legado.app.ui.watch

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemWatchBookBinding
import java.text.NumberFormat
import java.util.Locale

class WatchBookAdapter(
    private val onBookClick: (Book) -> Unit
) : RecyclerView.Adapter<WatchBookAdapter.Holder>() {

    private val books = arrayListOf<Book>()
    private val percentFormat = NumberFormat.getPercentInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(items: List<Book>) {
        books.clear()
        books.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemWatchBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = books.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(books[position])
    }

    inner class Holder(
        private val binding: ItemWatchBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) = binding.run {
            tvName.text = book.name.ifBlank { book.originName }
            tvProgress.text = progressText(book, itemView.context)
            root.setOnClickListener { onBookClick(book) }
        }
    }

    private fun progressText(book: Book, context: Context): String {
        val chapter = book.durChapterTitle.orEmpty().ifBlank { context.getString(R.string.watch_not_started) }
        val progress = if (book.totalChapterNum > 0) {
            percentFormat.format((book.durChapterIndex + 1).toDouble() / book.totalChapterNum.toDouble())
        } else {
            "0%"
        }
        return "$chapter · $progress"
    }
}
