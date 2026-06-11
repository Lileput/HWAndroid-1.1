package ru.netology.nmedia.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import ru.netology.nmedia.R
import java.io.File

object AttachmentUtils {

    const val MAX_ATTACHMENT_BYTES = 15L * 1024 * 1024

    fun validateSizeOrError(context: Context, file: File): String? =
        if (file.length() > MAX_ATTACHMENT_BYTES) {
            context.getString(R.string.error_attachment_too_large)
        } else {
            null
        }

    fun uriToFile(context: Context, uri: Uri): File? =
        runCatching {
            if (uri.scheme == "file") {
                uri.toFile()
            } else {
                val name = uri.lastPathSegment ?: "attachment"
                val temp = File(context.cacheDir, name)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                }
                temp
            }
        }.getOrNull()
}
