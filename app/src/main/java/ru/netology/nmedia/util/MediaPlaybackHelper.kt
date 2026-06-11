package ru.netology.nmedia.util

import android.content.Context
import ru.netology.nmedia.activity.MediaViewActivity

object MediaPlaybackHelper {

    fun play(context: Context, url: String?) {
        if (url.isNullOrBlank()) return
        val playbackUrl = ImageUrlResolver.playbackUrl(url)
        context.startActivity(MediaViewActivity.newIntent(context, playbackUrl))
    }
}
