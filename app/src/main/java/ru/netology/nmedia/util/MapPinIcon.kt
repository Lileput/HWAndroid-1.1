package ru.netology.nmedia.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.yandex.runtime.image.ImageProvider
import ru.netology.nmedia.R

object MapPinIcon {

    @Volatile
    private var cached: ImageProvider? = null

    fun imageProvider(context: Context): ImageProvider {
        return cached ?: create(context.applicationContext).also { cached = it }
    }

    private fun create(context: Context): ImageProvider {
        val sizePx = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val drawable = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_map_pin)) {
            "ic_map_pin missing"
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return ImageProvider.fromBitmap(bitmap)
    }
}
