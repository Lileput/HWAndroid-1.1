package ru.netology.nmedia.model

import android.net.Uri
import ru.netology.nmedia.dto.AttachmentType
import java.io.File

data class NewPostAttachment(
    val uri: Uri,
    val file: File,
    val type: AttachmentType,
)
