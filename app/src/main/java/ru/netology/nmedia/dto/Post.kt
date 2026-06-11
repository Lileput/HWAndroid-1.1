package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val author: String,
    val authorAvatar: String? = null,
    val authorId: Long,
    val authorJob: String? = null,
    val content: String,
    val published: String,
    val coords: Coordinates? = null,
    val link: String? = null,
    val mentionIds: List<Long> = emptyList(),
    val mentionedMe: Boolean = false,
    val likeOwnerIds: List<Long> = emptyList(),
    val likes: Int = 0,
    val shares: Int = 0,
    val views: Int = 0,
    val likedByMe: Boolean = false,
    val sharedByMe: Boolean = false,
    val video: String? = null,
    val attachment: Attachment? = null,
    val users: Map<String, PostUserPreview> = emptyMap(),
    val comments: Int = 0,
    val commentByMe: Boolean = false,
    val ownedByMe: Boolean = false,
) {
    val likesCount: Int
        get() = if (likes > 0) likes else likeOwnerIds.size
}

data class PostUserPreview(
    val name: String,
    val avatar: String? = null,
)

data class Attachment(
    val url: String,
    val type: AttachmentType,
)

enum class AttachmentType {
    IMAGE,
    VIDEO,
    AUDIO,
}
