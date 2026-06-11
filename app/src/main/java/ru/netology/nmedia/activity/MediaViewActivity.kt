package ru.netology.nmedia.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.api.PlaybackClient
import ru.netology.nmedia.databinding.ActivityMediaViewBinding
import ru.netology.nmedia.util.ImageUrlResolver
import ru.netology.nmedia.util.MediaDataSourceFactories
import ru.netology.nmedia.util.MediaFileCache
import ru.netology.nmedia.util.NetworkUtils
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MediaViewActivity : AppCompatActivity() {

    @Inject
    @PlaybackClient
    lateinit var playbackHttpClient: OkHttpClient

    private lateinit var binding: ActivityMediaViewBinding
    private var player: ExoPlayer? = null
    private var mediaUrl: String? = null
    private var playbackUrls: List<String> = emptyList()
    private var streamingUrlIndex = 0
    private var prepareJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            finish()
            return
        }
        mediaUrl = url
        playbackUrls = ImageUrlResolver.playbackCandidates(url)
        binding.progress.isVisible = true
        binding.toolbar.subtitle = if (MediaFileCache.isImageKitUrl(url)) {
            getString(R.string.media_loading_slow)
        } else {
            getString(R.string.media_loading)
        }
    }

    private fun releasePlayer() {
        binding.playerView.player = null
        player?.removeListener(playerListener)
        player?.release()
        player = null
    }

    private fun startPreparePipeline() {
        if (mediaUrl.isNullOrBlank() || playbackUrls.isEmpty()) return
        if (!NetworkUtils.isOnline(this)) {
            showPlaybackError(R.string.media_no_network)
            return
        }
        if (prepareJob?.isActive == true) return
        prepareJob = lifecycleScope.launch {
            binding.progress.isVisible = true
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "download pipeline start urls=$playbackUrls")
            }
            var localFile: File? = null
            var playedUrl: String? = null
            for (url in playbackUrls) {
                localFile = downloadWithRetries(url)
                if (localFile != null) {
                    playedUrl = url
                    break
                }
                if (!coroutineContext.isActive) return@launch
                if (playbackUrls.last() != url) {
                    runOnUiThread {
                        binding.toolbar.subtitle = getString(R.string.media_loading_alternate)
                    }
                }
            }
            if (isFinishing || !coroutineContext.isActive) return@launch
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch
            binding.playerView.post {
                if (localFile != null && playedUrl != null) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "play local file size=${localFile.length()} url=$playedUrl")
                    }
                    playLocalFile(localFile, playedUrl)
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "download failed, fallback to streaming urls=$playbackUrls")
                    }
                    streamingUrlIndex = 0
                    playStreaming(playbackUrls[streamingUrlIndex])
                }
            }
        }
    }

    private suspend fun downloadWithRetries(url: String): File? {
        repeat(DOWNLOAD_ATTEMPTS) { attempt ->
            if (!coroutineContext.isActive) return null
            if (attempt > 0) {
                MediaFileCache.clearCached(this@MediaViewActivity, url)
            }
            val file = try {
                withContext(Dispatchers.IO) {
                    withTimeout(DOWNLOAD_TIMEOUT_MS) {
                        MediaFileCache.getOrDownload(
                            this@MediaViewActivity,
                            url,
                            playbackHttpClient,
                        ) { downloaded, total ->
                            runOnUiThread { updateDownloadProgress(downloaded, total) }
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "download timeout attempt=${attempt + 1}")
                }
                null
            }
            if (file != null) return file
            if (attempt < DOWNLOAD_ATTEMPTS - 1) {
                runOnUiThread {
                    binding.toolbar.subtitle = getString(R.string.media_loading_retry)
                }
                delay(RETRY_DELAY_MS)
            }
        }
        return null
    }

    private fun updateDownloadProgress(downloaded: Long, total: Long) {
        binding.toolbar.subtitle = if (total > 0L) {
            getString(
                R.string.media_download_progress,
                megabytes(downloaded),
                megabytes(total),
            )
        } else {
            getString(R.string.media_download_progress_unknown, megabytes(downloaded))
        }
    }

    private fun megabytes(bytes: Long): String =
        String.format(Locale.US, "%.1f", bytes.toDouble() / (1024.0 * 1024.0))

    private fun buildPlayer(dataSourceFactory: DataSource.Factory): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(5_000, 50_000, 1_500, 2_500)
            .build()
        val factory = DefaultMediaSourceFactory(dataSourceFactory)
        return ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(factory)
            .build()
            .also { exoPlayer ->
                player = exoPlayer
                binding.playerView.player = exoPlayer
                exoPlayer.addListener(playerListener)
            }
    }

    private fun playLocalFile(file: File, originalUrl: String) {
        releasePlayer()
        val exoPlayer = buildPlayer(DefaultDataSource.Factory(this))
        exoPlayer.playWhenReady = true
        exoPlayer.setMediaItem(
            MediaItem.Builder()
                .setUri(Uri.fromFile(file))
                .setMimeType(mimeTypeFor(originalUrl))
                .build(),
        )
        exoPlayer.prepare()
    }

    private fun playStreaming(url: String) {
        releasePlayer()
        binding.progress.isVisible = true
        binding.toolbar.subtitle = getString(R.string.media_loading)
        val dataSourceFactory = MediaDataSourceFactories.forPlaybackUrl(
            context = this,
            url = url,
            authenticatedClient = playbackHttpClient,
        )
        val exoPlayer = buildPlayer(dataSourceFactory)
        exoPlayer.playWhenReady = true
        exoPlayer.setMediaItem(
            MediaItem.Builder()
                .setUri(url)
                .setMimeType(mimeTypeFor(url))
                .build(),
        )
        exoPlayer.prepare()
    }

    private fun mimeTypeFor(url: String): String =
        when {
            ImageUrlResolver.isAudioUrl(url) -> when {
                url.contains(".m4a", ignoreCase = true) -> MimeTypes.AUDIO_MP4
                url.contains(".aac", ignoreCase = true) -> MimeTypes.AUDIO_AAC
                url.contains(".ogg", ignoreCase = true) -> MimeTypes.AUDIO_OGG
                else -> MimeTypes.AUDIO_MPEG
            }
            else -> MimeTypes.VIDEO_MP4
        }

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "playWhenReady=$playWhenReady reason=$reason")
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "isPlaying=$isPlaying")
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                updateDurationLabel(player)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val exoPlayer = player ?: return
            if (BuildConfig.DEBUG) {
                Log.d(
                    TAG,
                    "state=$playbackState playWhenReady=${exoPlayer.playWhenReady} " +
                        "duration=${exoPlayer.duration}",
                )
            }
            updateDurationLabel(exoPlayer)
            when (playbackState) {
                Player.STATE_READY -> binding.progress.isVisible = false
                Player.STATE_BUFFERING -> binding.progress.isVisible = true
                Player.STATE_ENDED -> finish()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "playback error: ${error.errorCodeName}", error)
            }
            if (isNetworkPlaybackError(error) && streamingUrlIndex < playbackUrls.lastIndex) {
                streamingUrlIndex++
                binding.toolbar.subtitle = getString(R.string.media_loading_alternate)
                binding.progress.isVisible = true
                binding.playerView.post {
                    playStreaming(playbackUrls[streamingUrlIndex])
                }
                return
            }
            val messageRes = if (isNetworkPlaybackError(error) && !NetworkUtils.isOnline(this@MediaViewActivity)) {
                R.string.media_no_network
            } else {
                R.string.media_playback_error
            }
            showPlaybackError(messageRes)
        }
    }

    private fun isNetworkPlaybackError(error: PlaybackException): Boolean =
        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
            error.cause is java.net.UnknownHostException

    private fun showPlaybackError(messageRes: Int) {
        binding.progress.isVisible = false
        Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun updateDurationLabel(player: Player) {
        val durationMs = player.duration
        if (durationMs > 0 && durationMs != C.TIME_UNSET) {
            binding.toolbar.subtitle = formatDuration(durationMs)
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    override fun onStart() {
        super.onStart()
        binding.playerView.onResume()
    }

    override fun onResume() {
        super.onResume()
        binding.playerView.post {
            val exoPlayer = player
            if (exoPlayer != null && exoPlayer.mediaItemCount > 0) {
                exoPlayer.playWhenReady = true
                return@post
            }
            startPreparePipeline()
        }
    }

    override fun onPause() {
        binding.playerView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        prepareJob?.cancel()
        releasePlayer()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MediaViewActivity"
        private const val EXTRA_URL = "media_url"
        private const val DOWNLOAD_TIMEOUT_MS = 16 * 60 * 1000L
        private const val DOWNLOAD_ATTEMPTS = 2
        private const val RETRY_DELAY_MS = 3_000L

        fun newIntent(context: Context, url: String): Intent =
            Intent(context, MediaViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
    }
}
