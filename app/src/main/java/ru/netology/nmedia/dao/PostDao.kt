package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {

    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAll(): LiveData<List<PostEntity>>

    @Insert
    fun insert(post: PostEntity)

    @Query("UPDATE PostEntity SET content=:content WHERE id=:id")
    fun edit(id: Long, content: String)

    fun save(post: PostEntity) = if (post.id == 0L) {
        insert(post)
    } else {
        edit(post.id, post.content)
    }

    @Query(
        """
            UPDATE PostEntity SET
            likes = likes + CASE WHEN likedByMe = 1 THEN -1 ELSE 1 END,
            likedByMe = CASE WHEN likedByMe = 1 THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    fun likeById(id: Long)

    @Query("DELETE FROM PostEntity WHERE id=:id")
    fun removeById(id: Long)

    @Query(
        """
        UPDATE PostEntity SET
            share = share + CASE WHEN shareByMe THEN -1 ELSE 1 END,
            shareByMe = CASE WHEN shareByMe THEN 0 ELSE 1 END
        WHERE id = :id;
        """
    )
    fun reposts(id: Long)
}