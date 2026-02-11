package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {

    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAll(): LiveData<List<PostEntity>>

    @Query("SELECT * FROM PostEntity WHERE id = :id")
    suspend fun getByIdSync(id: Long): PostEntity?

    @Insert
    suspend fun insert(post: PostEntity)

    @Insert
    suspend fun insert(posts: List<PostEntity>)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM PostEntity")
    suspend fun clear()

    suspend fun clearAndInsert(posts: List<PostEntity>) {
        clear()
        insert(posts)
    }

    @Query("UPDATE PostEntity SET content = :content WHERE id = :id")
    suspend fun edit(id: Long, content: String)

    @Query(
        """
        UPDATE PostEntity SET
        likes = likes + CASE WHEN likedByMe = 1 THEN -1 ELSE 1 END,
        likedByMe = CASE WHEN likedByMe = 1 THEN 0 ELSE 1 END
        WHERE id = :id
        """
    )
    suspend fun likeById(id: Long)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query(
        """
        UPDATE PostEntity SET
        shares = shares + 1,
        sharedByMe = 1
        WHERE id = :id
        """
    )
    suspend fun reposts(id: Long)

    @Query("UPDATE PostEntity SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: PostEntity.SyncStatus)
}