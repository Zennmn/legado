package io.legado.app.help

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.Html
import android.util.Size
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.lang.ref.WeakReference
import io.legado.app.utils.SvgUtils
import java.util.concurrent.ConcurrentHashMap
import androidx.core.graphics.drawable.toDrawable
import android.graphics.Color
import com.bumptech.glide.request.RequestOptions
import java.io.ByteArrayInputStream
import kotlin.io.encoding.Base64

class GlideImageGetter(
    context: Context,
    textView: TextView,
    private val lifecycle: Lifecycle,
    private val availableWidth: Int,
    private val sourceOrigin: String? = null
) : Html.ImageGetter, Drawable.Callback {
    private val textViewRef = WeakReference(textView)
    private val contextRef = WeakReference(context)
    private val cacheDrawable = ConcurrentHashMap<String, GlideUrlDrawable>()
    private val pendingImages = mutableSetOf<String>()
    private val emptyDrawable by lazy {
        Color.TRANSPARENT.toDrawable()
    }

    override fun getDrawable(source: String?): Drawable {
        val context = contextRef.get()
        if (context == null || source.isNullOrBlank()) {
            return emptyDrawable
        }
        if (source.startsWith("data:image/svg")) {
            val inputStream =
                ByteArrayInputStream(Base64.decode(source.substringAfter(",")))
            val (pictureDrawable, size) = SvgUtils.createDrawable(inputStream)
                ?: return emptyDrawable
            pictureDrawable.bounds = Rect(0, 0, size.width, size.height)
            return pictureDrawable
        }
        cacheDrawable[source]?.let {
            return it
        }
        val urlDrawable = GlideUrlDrawable()
        cacheDrawable[source] = urlDrawable
        pendingImages.add(source)
        val target = ImageTarget(urlDrawable, source)
        Glide.with(context)
            .load(source)
            .into(target)
        return urlDrawable
    }

    private fun notifyImageLoaded(source: String) {
        pendingImages.remove(source)
        if (pendingImages.isEmpty()) {
            val textView = textViewRef.get() ?: return
            textView.text = textView.text
        }
    }

    override fun invalidateDrawable(who: Drawable) {
        textViewRef.get()?.invalidate()
    }

    override fun scheduleDrawable(
        who: Drawable,
        what: Runnable,
        `when`: Long
    ) {
    }

    override fun unscheduleDrawable(
        who: Drawable,
        what: Runnable
    ) {
    }

    private inner class GlideUrlDrawable() : Drawable(), Animatable {
        private var mDrawable: Drawable? = null
        private var gDrawable: GifDrawable? = null

        fun setDrawable(drawable: Drawable?) {
            if (drawable is GifDrawable) {
                gDrawable?.apply {
                    callback = null
                    stop()
                }
                gDrawable = drawable.apply {
                    if (!isRunning) {
                        callback = this@GlideImageGetter
                        start()
                    }
                }
                mDrawable = null
            } else {
                gDrawable?.apply {
                    callback = null
                    stop()
                }
                gDrawable = null
                mDrawable = drawable
            }
        }

        fun clear() {
            gDrawable?.apply {
                callback = null
                stop()
            }
            gDrawable = null
            mDrawable = null
        }

        override fun draw(canvas: Canvas) {
            mDrawable?.draw(canvas) ?: gDrawable?.draw(canvas)
        }

        override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
            mDrawable?.bounds = Rect(left, top, right, bottom)
            gDrawable?.bounds = Rect(left, top, right, bottom)
        }

        override fun setAlpha(alpha: Int) {
            mDrawable?.alpha = alpha
            gDrawable?.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            mDrawable?.colorFilter = colorFilter
            gDrawable?.colorFilter = colorFilter
        }

        override fun getOpacity(): Int {
            return PixelFormat.TRANSLUCENT
        }

        override fun start() {
            gDrawable?.start()
        }

        override fun stop() {
            gDrawable?.stop()
        }

        override fun isRunning(): Boolean {
            return gDrawable?.isRunning ?: false
        }
    }

    private inner class ImageTarget(
        private val urlDrawable: GlideUrlDrawable,
        private val source: String
    ) : CustomTarget<Drawable>() {

        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            urlDrawable.setDrawable(resource)
            invalidateDrawable(urlDrawable)
            notifyImageLoaded(source)
        }

        override fun onLoadCleared(placeholder: Drawable?) {
            urlDrawable.clear()
        }
    }
}
