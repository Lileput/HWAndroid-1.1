package ru.netology.nmedia.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Coordinates
import ru.netology.nmedia.dto.EventType
import ru.netology.nmedia.dto.PostUserPreview

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromLongList(value: List<Long>?): String = gson.toJson(value.orEmpty())

    @TypeConverter
    fun toLongList(value: String?): List<Long> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = EventType.valueOf(value)

    @TypeConverter
    fun fromCoordinates(value: Coordinates?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toCoordinates(value: String?): Coordinates? {
        if (value.isNullOrBlank()) return null
        return gson.fromJson(value, Coordinates::class.java)
    }

    @TypeConverter
    fun fromAttachment(value: Attachment?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toAttachment(value: String?): Attachment? {
        if (value.isNullOrBlank()) return null
        return gson.fromJson(value, Attachment::class.java)
    }

    @TypeConverter
    fun fromUsersMap(value: Map<String, PostUserPreview>?): String = gson.toJson(value.orEmpty())

    @TypeConverter
    fun toUsersMap(value: String?): Map<String, PostUserPreview> {
        if (value.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, PostUserPreview>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
}
