package ru.netology.nmedia.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import ru.netology.nmedia.R

object ImageLoader {
    private const val AVATAR_FALLBACK_DP = 48
    private const val POST_MEDIA_WIDTH_DP = 392
    private const val POST_MEDIA_HEIGHT_DP = 188

    private const val BIND_KEY_NONE = -1L

    fun loadAvatar(imageView: ImageView, avatarName: String?, bindKey: Long = BIND_KEY_NONE) {
        prepareListImageView(imageView, bindKey)
        if (avatarName.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_baseline_person_24)
            return
        }

        if (!isBoundToKey(imageView, bindKey)) return

        val target = ImageUrlResolver.avatarTarget(imageView, avatarName, AVATAR_FALLBACK_DP)
        markLoad(imageView, target.url)
        logStart("avatar", avatarName, target)

        Glide.with(imageView)
            .load(target.url)
            .apply(defaultOptions())
            .priority(Priority.LOW)
            .override(target.widthPx, target.heightPx)
            .placeholder(R.drawable.ic_baseline_person_24)
            .error(R.drawable.ic_baseline_person_24)
            .apply(RequestOptions.bitmapTransform(CircleCrop()))
            .listener(listItemListener(imageView, bindKey, "avatar", target.url))
            .into(imageView)
    }

    fun loadAttachmentImage(imageView: ImageView, fileName: String?, postId: Long) {
        prepareListImageView(imageView, postId)
        if (fileName.isNullOrBlank()) {
            imageView.visibility = View.GONE
            return
        }

        imageView.visibility = View.VISIBLE
        if (!isBoundToKey(imageView, postId)) return

        val target = ImageUrlResolver.mediaTarget(
            imageView,
            fileName,
            POST_MEDIA_WIDTH_DP,
            POST_MEDIA_HEIGHT_DP,
        )
        markLoad(imageView, target.url)
        logStart("attachment", fileName, target)

        Glide.with(imageView)
            .load(target.url)
            .apply(defaultOptions())
            .priority(Priority.NORMAL)
            .override(target.widthPx, target.heightPx)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_broken_image)
            .centerCrop()
            .listener(attachmentListener(imageView, target, postId))
            .into(imageView)
    }

    private fun loadAttachmentLarger(imageView: ImageView, originalUrl: String, postId: Long) {
        if (!isBoundToKey(imageView, postId)) return

        val target = ImageUrlResolver.detailMediaTarget(imageView, originalUrl)
        markLoad(imageView, target.url)
        ImageLoadLogger.d("attachment retry larger ${target.widthPx}x${target.heightPx}")

        Glide.with(imageView)
            .load(target.url)
            .apply(defaultOptions())
            .priority(Priority.NORMAL)
            .override(target.widthPx, target.heightPx)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_broken_image)
            .centerCrop()
            .listener(listItemListener(imageView, postId, "attachment", target.url))
            .into(imageView)
    }

    fun loadVideoPreview(imageView: ImageView, url: String?, postId: Long = BIND_KEY_NONE) {
        prepareListImageView(imageView, postId)
        if (url.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.video_placeholder)
            return
        }

        if (!isBoundToKey(imageView, postId)) return

        val target = if (ImageUrlResolver.isVideoUrl(url)) {
            ImageUrlResolver.videoThumbnailTarget(
                imageView,
                url,
                POST_MEDIA_WIDTH_DP,
                POST_MEDIA_HEIGHT_DP,
            )
        } else {
            ImageUrlResolver.mediaTarget(
                imageView,
                url,
                POST_MEDIA_WIDTH_DP,
                POST_MEDIA_HEIGHT_DP,
            )
        }
        markLoad(imageView, target.url)
        logStart("videoPreview", url, target)

        val useVideoFrame = ImageUrlResolver.isVideoUrl(url) &&
            !target.url.contains("/ik-thumbnail.jpg")
        val glideRequest = Glide.with(imageView)
            .asBitmap()
            .load(target.url)
            .apply(defaultOptions())
            .priority(Priority.LOW)
            .override(target.widthPx, target.heightPx)
            .placeholder(R.drawable.video_placeholder)
            .error(R.drawable.video_placeholder)
            .centerCrop()
        if (useVideoFrame) {
            glideRequest.frame(1_000_000)
        }
        glideRequest
            .listener(videoPreviewListener(imageView, bindKey = postId, originalUrl = url, target = target))
            .into(imageView)
    }

    fun loadDetailAttachmentImage(imageView: ImageView, url: String?) {
        if (url.isNullOrBlank()) {
            imageView.visibility = View.GONE
            return
        }
        imageView.visibility = View.VISIBLE
        loadDetailImage(
            imageView = imageView,
            url = url,
            kind = "detailAttachment",
            placeholderRes = R.drawable.ic_image_placeholder,
            errorRes = R.drawable.ic_broken_image,
        )
    }

    fun loadFullSizeImage(imageView: ImageView, url: String?) {
        if (url.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_error)
            return
        }
        loadDetailImage(
            imageView = imageView,
            url = url,
            kind = "fullSize",
            placeholderRes = R.drawable.ic_placeholder,
            errorRes = R.drawable.ic_error,
        )
    }

    private fun loadDetailImage(
        imageView: ImageView,
        url: String,
        kind: String,
        placeholderRes: Int,
        errorRes: Int,
    ) {
        val detail = ImageUrlResolver.detailMediaTarget(imageView, url)
        val preview = ImageUrlResolver.listPreviewTarget(imageView, url)
        markLoad(imageView, detail.url)
        logStart(kind, url, detail)

        Glide.with(imageView)
            .load(detail.url)
            .apply(detailOptions())
            .priority(Priority.HIGH)
            .override(detail.widthPx, detail.heightPx)
            .thumbnail(
                Glide.with(imageView)
                    .load(preview.url)
                    .apply(defaultOptions())
                    .onlyRetrieveFromCache(true)
                    .override(preview.widthPx, preview.heightPx),
            )
            .placeholder(placeholderRes)
            .error(errorRes)
            .fitCenter()
            .listener(detailListener(imageView, kind, detail, preview, placeholderRes, errorRes))
            .into(imageView)
    }

    fun clear(imageView: ImageView) {
        Glide.with(imageView).clear(imageView)
        imageView.setTag(R.id.image_post_id, null)
        imageView.setTag(R.id.image_load_url, null)
    }

    private fun prepareListImageView(imageView: ImageView, bindKey: Long) {
        if (bindKey == BIND_KEY_NONE) return
        Glide.with(imageView).clear(imageView)
        imageView.setTag(R.id.image_post_id, bindKey)
    }

    private fun isBoundToKey(imageView: ImageView, bindKey: Long): Boolean =
        bindKey == BIND_KEY_NONE || imageView.getTag(R.id.image_post_id) == bindKey

    private fun logStart(kind: String, original: String, target: ImageUrlResolver.Target) {
        ImageLoadLogger.logLoadStart(
            kind = kind,
            original = original,
            url = target.url,
            widthPx = target.widthPx,
            heightPx = target.heightPx,
            viewInfo = target.viewInfo,
        )
    }

    private fun markLoad(imageView: ImageView, url: String) {
        imageView.setTag(R.id.image_load_url, url)
    }

    private fun isCurrentLoad(imageView: ImageView, url: String): Boolean =
        imageView.getTag(R.id.image_load_url) == url

    private fun defaultOptions(): RequestOptions =
        RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .timeout(22_000)

    private fun detailOptions(): RequestOptions =
        RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .timeout(35_000)

    private fun attachmentListener(
        imageView: ImageView,
        target: ImageUrlResolver.Target,
        postId: Long,
    ): RequestListener<Drawable> {
        var retried = false
        val startedAt = System.currentTimeMillis()
        return object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                glideTarget: Target<Drawable>,
                isFirstResource: Boolean,
            ): Boolean {
                if (!isBoundToKey(imageView, postId)) {
                    ImageLoadLogger.d("attachment ignore stale fail")
                    return true
                }
                logFailure("attachment", target.url, startedAt, e)
                if (!retried) {
                    retried = true
                    imageView.post {
                        if (!isBoundToKey(imageView, postId)) return@post
                        loadAttachmentLarger(imageView, target.originalUrl, postId)
                    }
                    return true
                }
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                glideTarget: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean {
                if (!isBoundToKey(imageView, postId)) return true
                ImageLoadLogger.logLoadSuccess(
                    kind = "attachment",
                    url = target.url,
                    durationMs = System.currentTimeMillis() - startedAt,
                )
                ImageLoadLogger.d("source=$dataSource")
                return false
            }
        }
    }

    private fun videoPreviewListener(
        imageView: ImageView,
        bindKey: Long,
        originalUrl: String,
        target: ImageUrlResolver.Target,
    ): RequestListener<Bitmap> {
        var retriedWithFrame = false
        val startedAt = System.currentTimeMillis()
        return object : RequestListener<Bitmap> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                glideTarget: Target<Bitmap>,
                isFirstResource: Boolean,
            ): Boolean {
                if (!isBoundToKey(imageView, bindKey)) return true
                logFailure("videoPreview", target.url, startedAt, e)
                if (!retriedWithFrame && ImageUrlResolver.isVideoUrl(originalUrl)) {
                    retriedWithFrame = true
                    val fallbackUrl = ImageUrlResolver.playbackUrl(originalUrl)
                    imageView.post {
                        if (!isBoundToKey(imageView, bindKey)) return@post
                        markLoad(imageView, fallbackUrl)
                        Glide.with(imageView)
                            .asBitmap()
                            .load(fallbackUrl)
                            .apply(defaultOptions())
                            .priority(Priority.LOW)
                            .override(target.widthPx, target.heightPx)
                            .frame(1_000_000)
                            .placeholder(R.drawable.video_placeholder)
                            .error(R.drawable.video_placeholder)
                            .centerCrop()
                            .into(imageView)
                    }
                    return true
                }
                return false
            }

            override fun onResourceReady(
                resource: Bitmap,
                model: Any,
                glideTarget: Target<Bitmap>?,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean {
                if (!isBoundToKey(imageView, bindKey)) return true
                ImageLoadLogger.logLoadSuccess(
                    kind = "videoPreview",
                    url = target.url,
                    durationMs = System.currentTimeMillis() - startedAt,
                )
                return false
            }
        }
    }

    private fun listItemListener(
        imageView: ImageView,
        bindKey: Long,
        kind: String,
        url: String,
    ): RequestListener<Drawable> {
        val startedAt = System.currentTimeMillis()
        return object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                glideTarget: Target<Drawable>,
                isFirstResource: Boolean,
            ): Boolean {
                if (!isBoundToKey(imageView, bindKey)) return true
                logFailure(kind, url, startedAt, e)
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                glideTarget: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean {
                if (!isBoundToKey(imageView, bindKey)) return true
                ImageLoadLogger.logLoadSuccess(
                    kind = kind,
                    url = url,
                    durationMs = System.currentTimeMillis() - startedAt,
                )
                ImageLoadLogger.d("source=$dataSource")
                return false
            }
        }
    }

    private fun detailListener(
        imageView: ImageView,
        kind: String,
        detail: ImageUrlResolver.Target,
        preview: ImageUrlResolver.Target,
        placeholderRes: Int,
        errorRes: Int,
    ): RequestListener<Drawable> {
        var fallbackUsed = false
        val startedAt = System.currentTimeMillis()
        return object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                glideTarget: Target<Drawable>,
                isFirstResource: Boolean,
            ): Boolean {
                if (!isCurrentLoad(imageView, detail.url)) {
                    ImageLoadLogger.d("$kind ignore stale fail")
                    return true
                }
                logFailure(kind, detail.url, startedAt, e)
                if (!fallbackUsed) {
                    fallbackUsed = true
                    ImageLoadLogger.d("$kind fallback -> list preview")
                    imageView.post {
                        if (!isCurrentLoad(imageView, detail.url)) return@post
                        markLoad(imageView, preview.url)
                        Glide.with(imageView)
                            .load(preview.url)
                            .apply(defaultOptions())
                            .priority(Priority.HIGH)
                            .override(preview.widthPx, preview.heightPx)
                            .placeholder(placeholderRes)
                            .error(errorRes)
                            .fitCenter()
                            .into(imageView)
                    }
                    return true
                }
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                glideTarget: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean {
                if (!isCurrentLoad(imageView, detail.url)) return true
                ImageLoadLogger.logLoadSuccess(
                    kind = kind,
                    url = detail.url,
                    durationMs = System.currentTimeMillis() - startedAt,
                )
                ImageLoadLogger.d("source=$dataSource")
                return false
            }
        }
    }

    private fun logFailure(kind: String, url: String, startedAt: Long, e: GlideException?) {
        val rootMessage = e?.rootCauses?.firstOrNull()?.message.orEmpty()
        val canceled = rootMessage.contains("Canceled", ignoreCase = true)
        ImageLoadLogger.logLoadFailed(
            kind = kind,
            url = url,
            durationMs = System.currentTimeMillis() - startedAt,
            error = rootMessage.ifBlank { e?.message },
            canceled = canceled,
        )
    }
}
