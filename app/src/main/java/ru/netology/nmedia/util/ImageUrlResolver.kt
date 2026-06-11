package ru.netology.nmedia.util

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlin.math.roundToInt

object ImageUrlResolver {
    private const val BASE_URL = "http://94.228.125.136:8080"
    private const val IMAGEKIT_HOST = "ik.imagekit.io"

    private const val MAX_AVATAR_PX = 128
    private const val MAX_POST_MEDIA_WIDTH_PX = 560
    private const val MAX_POST_MEDIA_HEIGHT_PX = 280
    private const val MAX_DETAIL_WIDTH_PX = 960
    private const val MAX_DETAIL_HEIGHT_PX = 720
    private const val POST_MEDIA_ASPECT = 188f / 392f

    data class Target(
        val url: String,
        val widthPx: Int,
        val heightPx: Int,
        val viewInfo: String,
        val originalUrl: String = url,
    )

    fun resolve(value: String, path: String): String =
        if (value.startsWith("http://") || value.startsWith("https://")) {
            value
        } else {
            "$BASE_URL$path$value"
        }

    fun avatarTarget(imageView: ImageView, avatar: String, fallbackDp: Int = 48): Target {
        val widthPx = cappedSizePx(imageView, fallbackDp, MAX_AVATAR_PX)
        val resolved = resolve(avatar, "/avatars/")
        val url = withImageKitTransform(resolved, widthPx, widthPx)
        return Target(url, widthPx, widthPx, viewInfo(imageView), originalUrl = avatar)
    }

    fun detailMediaTarget(imageView: ImageView, url: String): Target {
        val dm = imageView.resources.displayMetrics
        val widthPx = dm.widthPixels.coerceIn(1, MAX_DETAIL_WIDTH_PX)
        val heightPx = (widthPx * POST_MEDIA_ASPECT).roundToInt().coerceIn(1, MAX_DETAIL_HEIGHT_PX)
        val resolved = resolve(stripImageKitTransform(url), "/media/")
        val transformed = withImageKitTransform(resolved, widthPx, heightPx)
        return Target(
            url = transformed,
            widthPx = widthPx,
            heightPx = heightPx,
            viewInfo = "detail ${widthPx}x$heightPx screen=${dm.widthPixels}x${dm.heightPixels}",
            originalUrl = url,
        )
    }

    fun listPreviewTarget(imageView: ImageView, url: String): Target =
        mediaTarget(imageView, stripImageKitTransform(url), 392, 188)

    fun mediaTarget(
        imageView: ImageView,
        url: String,
        fallbackWidthDp: Int,
        fallbackHeightDp: Int,
    ): Target {
        val widthPx = cappedSizePx(imageView, fallbackWidthDp, MAX_POST_MEDIA_WIDTH_PX)
        val heightPx = dpToPx(imageView, fallbackHeightDp).coerceIn(1, MAX_POST_MEDIA_HEIGHT_PX)
        val resolved = resolve(url, "/media/")
        val transformed = withImageKitTransform(resolved, widthPx, heightPx)
        return Target(
            url = transformed,
            widthPx = widthPx,
            heightPx = heightPx,
            viewInfo = viewInfo(imageView),
            originalUrl = url,
        )
    }

    fun videoThumbnailTarget(
        imageView: ImageView,
        url: String,
        fallbackWidthDp: Int,
        fallbackHeightDp: Int,
    ): Target {
        val widthPx = cappedSizePx(imageView, fallbackWidthDp, MAX_POST_MEDIA_WIDTH_PX)
        val heightPx = dpToPx(imageView, fallbackHeightDp).coerceIn(1, MAX_POST_MEDIA_HEIGHT_PX)
        val resolved = resolve(url, "/media/")
        val thumbUrl = withImageKitVideoThumbnail(resolved, widthPx, heightPx)
        return Target(
            url = thumbUrl,
            widthPx = widthPx,
            heightPx = heightPx,
            viewInfo = viewInfo(imageView),
            originalUrl = url,
        )
    }

    fun isVideoUrl(url: String): Boolean = url.matches(VIDEO_EXTENSIONS)

    fun isAudioUrl(url: String): Boolean = url.matches(AUDIO_EXTENSIONS)

    fun playbackUrl(url: String): String = resolve(stripImageKitTransform(url), "/media/")

    fun playbackCandidates(originalUrl: String): List<String> {
        val primary = playbackUrl(originalUrl)
        val candidates = linkedSetOf(primary)
        apiPlaybackFallback(primary)?.let { candidates.add(it) }
        if (originalUrl != primary) {
            apiPlaybackFallback(originalUrl)?.let { candidates.add(it) }
        }
        return candidates.toList()
    }

    fun apiPlaybackFallback(url: String): String? {
        if (!url.contains(IMAGEKIT_HOST)) return null
        val path = url.substringAfter(IMAGEKIT_HOST).trimStart('/')
        val fileName = path.substringAfter('/', missingDelimiterValue = "").substringBefore('?')
        if (fileName.isBlank() || !fileName.contains('.')) return null
        return resolve(fileName, "/media/")
    }

    fun stripImageKitTransform(url: String): String {
        if (!url.contains("/tr:")) {
            return url
        }
        return url.replace(Regex("""/tr:[^/]+/"""), "/")
    }

    fun withImageKitTransform(url: String, widthPx: Int, heightPx: Int): String {
        if (!url.contains(IMAGEKIT_HOST) || url.contains("/tr:")) {
            return url
        }
        if (!url.matches(IMAGE_EXTENSIONS)) {
            return url
        }
        return buildImageKitTransformUrl(url, "w-$widthPx,h-$heightPx,c-at_max")
    }

    fun withImageKitVideoThumbnail(url: String, widthPx: Int, heightPx: Int): String {
        if (!url.contains(IMAGEKIT_HOST) || url.contains("/tr:")) {
            return url
        }
        if (!url.matches(VIDEO_EXTENSIONS)) {
            return url
        }
        val thumbnailPath = imageKitVideoThumbnailAsset(url)
        return buildImageKitTransformUrl(
            thumbnailPath,
            "so-1,w-$widthPx,h-$heightPx,c-at_max,f-jpg",
        )
    }

    private fun imageKitVideoThumbnailAsset(videoUrl: String): String {
        if (videoUrl.contains("/ik-thumbnail.jpg")) {
            return videoUrl
        }
        return "$videoUrl/ik-thumbnail.jpg"
    }

    private fun buildImageKitTransformUrl(url: String, transform: String): String {
        val prefix = "https://$IMAGEKIT_HOST/"
        if (!url.startsWith(prefix)) {
            return url
        }
        val afterHost = url.removePrefix(prefix)
        val slashIndex = afterHost.indexOf('/')
        if (slashIndex <= 0) {
            return url
        }
        val imageKitId = afterHost.substring(0, slashIndex)
        val filePath = afterHost.substring(slashIndex + 1)
        return "${prefix}${imageKitId}/tr:$transform/$filePath"
    }

    private fun cappedSizePx(view: View, fallbackDp: Int, maxPx: Int): Int {
        val lp = view.layoutParams
        val fromLayout = lp?.width?.takeIf { isConcreteSize(it) }
        val px = when {
            fromLayout != null -> fromLayout
            else -> dpToPx(view, fallbackDp)
        }
        return px.coerceIn(1, maxPx)
    }

    private fun isConcreteSize(size: Int): Boolean =
        size > 0 && size != ViewGroup.LayoutParams.MATCH_PARENT

    fun dpToPx(view: View, dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            view.resources.displayMetrics,
        ).roundToInt()

    private fun viewInfo(view: View): String {
        val lp = view.layoutParams
        return "layout=${lp?.width}x${lp?.height} measured=${view.width}x${view.height}"
    }

    private val IMAGE_EXTENSIONS =
        Regex(".*\\.(jpg|jpeg|png|webp|gif)(\\?.*)?$", RegexOption.IGNORE_CASE)

    private val VIDEO_EXTENSIONS =
        Regex(".*\\.(mp4|webm|mov|m4v|mkv|3gp)(\\?.*)?$", RegexOption.IGNORE_CASE)

    private val AUDIO_EXTENSIONS =
        Regex(".*\\.(mp3|m4a|aac|ogg|wav|flac)(\\?.*)?$", RegexOption.IGNORE_CASE)
}
