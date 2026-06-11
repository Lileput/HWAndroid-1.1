package ru.netology.nmedia.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSingleEventBinding
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Coordinates
import ru.netology.nmedia.dto.Event
import ru.netology.nmedia.dto.EventType
import ru.netology.nmedia.util.Formatter
import ru.netology.nmedia.util.ImageLoader
import ru.netology.nmedia.util.LinkUtils
import ru.netology.nmedia.util.LongArg
import ru.netology.nmedia.util.MapHelper
import ru.netology.nmedia.util.UserAvatarUiHelper
import ru.netology.nmedia.util.YandexMapLifecycle
import ru.netology.nmedia.util.registerMapLocationPermissionRequest
import ru.netology.nmedia.viewModel.SingleEventViewModel

@AndroidEntryPoint
class SingleEventFragment : Fragment() {

    private val viewModel: SingleEventViewModel by viewModels()

    private var _binding: FragmentSingleEventBinding? = null
    private val binding get() = _binding!!

    private var mapCoords: Coordinates? = null
    private var readOnlyMapConfigured = false
    private lateinit var requestMapLocationPermission: () -> Unit
    private var currentEvent: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMapLocationPermission = registerMapLocationPermissionRequest()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSingleEventBinding.inflate(inflater, container, false)
        val eventId = arguments?.eventId ?: 0L

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progress.isVisible = loading
            binding.scroll.isVisible = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction(R.string.navigate_up) { findNavController().navigateUp() }
                    .show()
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            if (event == null) return@observe
            bindEvent(event)
        }

        if (savedInstanceState == null) {
            viewModel.load(eventId)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.event_details)
            setDisplayHomeAsUpEnabled(true)
        }

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_single_post, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.share -> {
                            val event = currentEvent ?: return false
                            shareEvent(event.content)
                            true
                        }
                        else -> false
                    }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
    }

    private fun shareEvent(content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.chooser_share_event)))
    }

    private fun bindEvent(event: Event) {
        currentEvent = event
        binding.author.text = event.author
        binding.authorJob.text = event.authorJob?.takeIf { it.isNotBlank() }
            ?: getString(R.string.looking_for_job)
        ru.netology.nmedia.util.ImageLoader.loadAvatar(binding.avatar, event.authorAvatar)

        binding.eventType.text = when (event.type) {
            EventType.ONLINE -> getString(R.string.event_type_online)
            EventType.OFFLINE -> getString(R.string.event_type_offline)
        }
        binding.eventDatetime.text = Formatter.formatDateTime(event.datetime)
        LinkUtils.bindTextWithLinks(binding.content, event.content)

        val separateLink = event.link?.takeIf { url ->
            url.isNotBlank() && !event.content.contains(url, ignoreCase = true)
        }
        binding.link.isVisible = separateLink != null
        if (separateLink != null) {
            binding.link.text = separateLink
            binding.link.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, separateLink.toUri()))
            }
        }

        bindAttachments(event)
        bindSpeakers(event)
        bindLikers(event)
        bindParticipants(event)
        bindMap(event)
    }

    private fun bindLikers(event: Event) {
        val likerIds = event.likeOwnerIds
        val hasLikers = likerIds.isNotEmpty()
        binding.likersTitle.isVisible = hasLikers
        binding.likersRow.isVisible = hasLikers
        if (!hasLikers) return

        binding.likersCount.text = event.likesCount.toString()
        val chips = likerIds.map { id ->
            val preview = event.users[id.toString()]
            UserAvatarUiHelper.ChipUser(id, preview?.avatar)
        }
        UserAvatarUiHelper.bindAvatars(
            container = binding.likersAvatars,
            users = chips,
            onMoreClick = if (likerIds.size > 5) {
                {
                    findNavController().navigate(
                        R.id.action_singleEventFragment_to_postLikersFragment,
                        bundleOf(
                            PostLikersFragment.ARG_USER_IDS to likerIds.toLongArray(),
                            PostLikersFragment.ARG_TITLE_RES to R.string.likers,
                        ),
                    )
                }
            } else {
                null
            },
        )
    }

    private fun bindAttachments(event: Event) {
        binding.attachmentContainer.isVisible = false
        binding.videoContainer.isVisible = false
        binding.audioPlay.isVisible = false
        binding.audioPlay.setOnClickListener(null)

        when (event.attachment?.type) {
            AttachmentType.IMAGE -> {
                binding.attachmentContainer.isVisible = true
                ImageLoader.loadDetailAttachmentImage(binding.attachmentImage, event.attachment.url)
                val open = View.OnClickListener {
                    findNavController().navigate(
                        R.id.action_singleEventFragment_to_photoViewFragment,
                        PhotoViewFragment.createArguments(event.attachment.url),
                    )
                }
                binding.attachmentContainer.setOnClickListener(open)
                binding.attachmentImage.setOnClickListener(open)
            }
            AttachmentType.VIDEO -> {
                binding.videoContainer.isVisible = true
                ImageLoader.loadVideoPreview(binding.videoPreview, event.attachment.url)
                val play = View.OnClickListener { playMedia(event.attachment.url) }
                binding.videoContainer.setOnClickListener(play)
                binding.playButton.setOnClickListener(play)
            }
            AttachmentType.AUDIO -> {
                binding.audioPlay.isVisible = true
                binding.audioPlay.setOnClickListener { playMedia(event.attachment.url) }
            }
            null -> Unit
        }
    }

    private fun bindSpeakers(event: Event) {
        val speakerIds = event.speakerIds
        val hasSpeakers = speakerIds.isNotEmpty()
        binding.speakersTitle.isVisible = hasSpeakers
        binding.speakersRow.isVisible = hasSpeakers
        if (!hasSpeakers) return

        val chips = speakerIds.mapNotNull { id ->
            event.users[id.toString()]?.let { preview ->
                UserAvatarUiHelper.ChipUser(id, preview.avatar)
            }
        }
        UserAvatarUiHelper.bindAvatars(
            container = binding.speakersAvatars,
            users = chips,
            onMoreClick = null,
        )
    }

    private fun bindParticipants(event: Event) {
        val participantIds = event.participantsIds
        val hasParticipants = participantIds.isNotEmpty()
        binding.participantsTitle.isVisible = hasParticipants
        binding.participantsRow.isVisible = hasParticipants
        if (!hasParticipants) return

        binding.participantsCount.text = event.participantsCount.toString()
        val chips = participantIds.mapNotNull { id ->
            event.users[id.toString()]?.let { preview ->
                UserAvatarUiHelper.ChipUser(id, preview.avatar)
            }
        }
        UserAvatarUiHelper.bindAvatars(
            container = binding.participantsAvatars,
            users = chips,
            onMoreClick = null,
        )
    }

    override fun onStart() {
        super.onStart()
        if (BuildConfig.MAPKIT_API_KEY.isBlank()) return
        YandexMapLifecycle.startMap(binding.mapView)
        configureReadOnlyMap()
    }

    override fun onStop() {
        if (BuildConfig.MAPKIT_API_KEY.isNotBlank()) {
            YandexMapLifecycle.stopMap(binding.mapView)
            readOnlyMapConfigured = false
        }
        super.onStop()
    }

    private fun bindMap(event: Event) {
        val coords = event.coords
        binding.mapView.isVisible = coords != null
        if (coords == null) {
            mapCoords = null
            readOnlyMapConfigured = false
            return
        }

        mapCoords = coords
        readOnlyMapConfigured = false
        requestMapLocationPermission()

        val geoUri = "geo:${coords.lat},${coords.long}?q=${coords.lat},${coords.long}".toUri()
        binding.mapView.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, geoUri))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(requireContext(), R.string.no_map_app, Toast.LENGTH_SHORT).show()
            }
        }

        if (BuildConfig.MAPKIT_API_KEY.isNotBlank()) {
            configureReadOnlyMap()
        }
    }

    private fun configureReadOnlyMap() {
        val coords = mapCoords ?: return
        if (readOnlyMapConfigured) return
        readOnlyMapConfigured = true
        MapHelper.setupReadOnlyMap(requireContext(), binding.mapView, coords)
    }

    private fun playMedia(url: String) {
        ru.netology.nmedia.util.MediaPlaybackHelper.play(requireContext(), url)
    }

    override fun onDestroyView() {
        currentEvent = null
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        super.onDestroyView()
        _binding = null
    }

    companion object {
        var Bundle.eventId: Long by LongArg
    }
}
