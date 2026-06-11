package ru.netology.nmedia.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import ru.netology.nmedia.R
import java.io.File
import java.io.InputStream

object AvatarValidator {

    const val MAX_SIZE_PX = 2048

    const val EXTRA_FILE_PATH = "extra.file_path"

    private val allowedMimeTypes = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
    )

    private val allowedExtensions = setOf("jpg", "jpeg", "png")

    fun validate(context: Context, uri: Uri, filePath: String? = null): String? {
        if (!isAllowedFormat(context, uri, filePath)) {
            return context.getString(R.string.error_avatar_format)
        }

        val options = readImageBounds(context, uri, filePath)
            ?: return context.getString(R.string.error_avatar_invalid)

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return context.getString(R.string.error_avatar_invalid)
        }
        if (options.outWidth > MAX_SIZE_PX || options.outHeight > MAX_SIZE_PX) {
            return context.getString(R.string.error_avatar_size)
        }
        return null
    }

    private fun isAllowedFormat(context: Context, uri: Uri, filePath: String?): Boolean {
        val mime = context.contentResolver.getType(uri)
        if (mime != null) {
            return mime in allowedMimeTypes
        }
        val extension = filePath?.substringAfterLast('.', "")?.lowercase()
            ?: uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
        return extension == null || extension in allowedExtensions
    }

    private fun readImageBounds(
        context: Context,
        uri: Uri,
        filePath: String?,
    ): BitmapFactory.Options? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }

        if (!filePath.isNullOrBlank()) {
            val file = File(filePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath, options)
                if (options.outWidth > 0 && options.outHeight > 0) {
                    return options
                }
            }
        }

        openInputStream(context, uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                return options
            }
        }

        return null
    }

    private fun openInputStream(context: Context, uri: Uri): InputStream? {
        return when (uri.scheme?.lowercase()) {
            "file" -> {
                val path = uri.path ?: return null
                File(path).takeIf { it.exists() }?.inputStream()
            }
            "content" -> context.contentResolver.openInputStream(uri)
            else -> context.contentResolver.openInputStream(uri)
        }
    }
}
