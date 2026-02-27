package ru.netology.nmedia.util

import android.util.Log
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import ru.netology.nmedia.R

object ImageLoader {
    private const val BASE_URL = "http://10.0.2.2:9999"
    fun loadAvatar(imageView: ImageView, avatarName: String?) {

        if (avatarName.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_baseline_person_24)
            return
        }

        val avatarUrl = "$BASE_URL/avatars/$avatarName"

        try {
            Glide.with(imageView.context)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_baseline_person_24)
                .error(R.drawable.ic_baseline_person_24)
                .transition(DrawableTransitionOptions.withCrossFade())
                .timeout(10_000)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(imageView)
        } catch (e: Exception) {
            imageView.setImageResource(R.drawable.ic_baseline_person_24)
        }
    }

    fun loadAttachmentImage(imageView: ImageView, fileName: String?) {

        if (fileName.isNullOrBlank()) {
            imageView.visibility = View.GONE
            return
        }

        val imageUrl = "$BASE_URL/media/$fileName"

        Glide.with(imageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_broken_image)
            .transition(DrawableTransitionOptions.withCrossFade())
            .timeout(10_000)
            .into(imageView)
    }

    fun loadFullSizeImage(imageView: ImageView, url: String?) {

        if (url.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_error)
            return
        }

        Glide.with(imageView.context)
            .load(url)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_error)
            .fitCenter()
            .timeout(10_000)
            .into(imageView)
    }
}