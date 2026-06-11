package ru.netology.nmedia.dto

data class Event(
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String? = null,
    val authorJob: String? = null,
    val content: String,
    val datetime: String,
    val published: String,
    val coords: Coordinates? = null,
    val type: EventType,
    val likeOwnerIds: List<Long> = emptyList(),
    val likedByMe: Boolean = false,
    val speakerIds: List<Long> = emptyList(),
    val participantsIds: List<Long> = emptyList(),
    val participatedByMe: Boolean = false,
    val attachment: Attachment? = null,
    val link: String? = null,
    val users: Map<String, PostUserPreview> = emptyMap(),
    val ownedByMe: Boolean = false,
) {
    val likesCount: Int
        get() = likeOwnerIds.size

    val participantsCount: Int
        get() = participantsIds.size
}

enum class EventType {
    ONLINE,
    OFFLINE,
}
