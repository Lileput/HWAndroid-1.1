package ru.netology.nmedia.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import ru.netology.nmedia.R
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.PushToken
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class FCMService() : FirebaseMessagingService() {

    @Inject
    lateinit var firebaseMessaging: FirebaseMessaging

    private val action = "action"
    private val content = "content"
    private val gson = Gson()
    private val channelId = "remote"

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_remote_name)
            val descriptionText = getString(R.string.channel_remote_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val contentJson = message.data[content]
        if (contentJson == null) { return }

        val json = JSONObject(contentJson)
        val recipientId = if (json.has("recipientId") && !json.isNull("recipientId")) {
            json.optLong("recipientId")
        } else {
            null
        }
        val currentUserId = appAuth.authState.value?.id ?: 0L

        when {
            recipientId == null || recipientId == currentUserId -> {
                showSimpleNotification(json.optString("content", "Новое уведомление"))
            }
            else -> {
                resendPushToken()
            }
        }
    }

    private fun showSimpleNotification(content: String) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Новое уведомление")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notify(notification)
    }

    private fun processMessage(message: RemoteMessage) {
        message.data[action]?.let { action ->
            when (Action.fromValue(action)) {
                Action.LIKE -> handleLike(
                    gson.fromJson(message.data[content], Like::class.java)
                )
                Action.NEW_POST -> handleNewPost(
                    gson.fromJson(message.data[content], NewPost::class.java)
                )
                Action.UNKNOWN -> {
                }
            }
        } ?: run {
            Log.w("FCMService", "Message received without action field: ${message.data}")
        }
    }

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface FCMEntryPoint {
        fun getApiService(): PostApiService
    }

    private fun resendPushToken() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val entryPoint = EntryPointAccessors.fromApplication(
                    applicationContext,
                    FCMEntryPoint::class.java
                )
                val token = firebaseMessaging.token.await()
                entryPoint.getApiService().sendPushToken(PushToken(token))
                Log.d("FCMService", "Push token resent")
            }.onFailure {
                Log.e("FCMService", "Failed to resend push token", it)
            }
        }
    }

    private fun handleLike(like: Like) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(
                    R.string.notification_user_like,
                    like.userName,
                    like.postAuthor,
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notify(notification)
    }

    private fun handleNewPost(newPost: NewPost) {

        val shortText = if (newPost.postContent.length > 60) {
            newPost.postContent.take(60) + "..."
        } else {
            newPost.postContent
        }

        val expandedText = if (newPost.postContent.length > 150) {
            newPost.postContent.take(150) + "..."
        } else {
            newPost.postContent
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(expandedText)
            .setBigContentTitle(
                getString(
                    R.string.notification_new_post,
                    newPost.authorName
                )
            )
            .setSummaryText("")

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(
                    R.string.notification_new_post,
                    newPost.authorName
                )
            )
            .setContentText(shortText)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notify(notification)
    }

    private fun notify(notification: Notification) {
        val isUpperTiramisu = Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU
        val isPostNotificationGranted = if (isUpperTiramisu) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        if (isPostNotificationGranted) {
            NotificationManagerCompat.from(this).notify(Random.nextInt(100_000), notification)
        }
    }

    override fun onNewToken(token: String) {
        appAuth.sendPushToken(token)
    }

    enum class Action {
        LIKE,
        NEW_POST,
        UNKNOWN;

        companion object {
            fun fromValue(value: String): Action {
                return try {
                    valueOf(value)
                } catch (e: IllegalArgumentException) {
                    UNKNOWN
                }
            }
        }
    }

    data class Like(
        val userId: Long,
        val userName: String,
        val postId: Long,
        val postAuthor: String,
    )

    data class NewPost(
        val authorName: String,
        val postContent: String,
        val postId: Long
    )

}