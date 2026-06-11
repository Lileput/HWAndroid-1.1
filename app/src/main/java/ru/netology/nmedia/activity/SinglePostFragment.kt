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
import androidx.lifecycle.Lifecycle
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSinglePostBinding
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.util.Formatter
import ru.netology.nmedia.util.ImageLoader
import ru.netology.nmedia.util.LinkUtils
import ru.netology.nmedia.util.LongArg
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.dto.Coordinates
import ru.netology.nmedia.util.MapHelper
import ru.netology.nmedia.util.YandexMapLifecycle
import ru.netology.nmedia.util.UserAvatarUiHelper
import ru.netology.nmedia.util.registerMapLocationPermissionRequest
import ru.netology.nmedia.viewModel.SinglePostViewModel

@AndroidEntryPoint
class SinglePostFragment : Fragment() {

    private val viewModel: SinglePostViewModel by viewModels()

    private var _binding: FragmentSinglePostBinding? = null
    private val binding get() = _binding!!

    private var mapCoords: Coordinates? = null
    private var readOnlyMapConfigured = false
    private lateinit var requestMapLocationPermission: () -> Unit
    private var currentPost: Post? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMapLocationPermission = registerMapLocationPermissionRequest()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSinglePostBinding.inflate(inflater, container, false)
        val postId = arguments?.postId ?: 0L

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

        viewModel.post.observe(viewLifecycleOwner) { post ->
            if (post == null) return@observe
            bindPost(post)
        }

        if (savedInstanceState == null) {
            viewModel.load(postId)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_single_post, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.share -> {
                            val post = currentPost ?: return false
                            viewModel.repost(post.id)
                            sharePost(post.content)
                            true
                        }
                        else -> false
                    }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
    }

    override fun onResume() {
        super.onResume()
        updateActionBar()
    }

    private fun updateActionBar() {
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.post_details)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun sharePost(content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.chooser_share_post)))
    }

    private fun bindPost(post: Post) {
        currentPost = post
        binding.author.text = post.author
        binding.authorJob.text = post.authorJob?.takeIf { it.isNotBlank() }
            ?: getString(R.string.looking_for_job)
        ImageLoader.loadAvatar(binding.avatar, post.authorAvatar)

        binding.published.text = Formatter.formatPostDateTime(PostEntity.publishedToEpoch(post.published))
        LinkUtils.bindTextWithLinks(binding.content, post.content)

        val separateLink = post.link?.takeIf { url ->
            url.isNotBlank() && !post.content.contains(url, ignoreCase = true)
        }
        binding.link.isVisible = separateLink != null
        if (separateLink != null) {
            binding.link.text = separateLink
            binding.link.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, separateLink.toUri()))
            }
        }

        bindAttachments(post)
        bindLikers(post)
        bindMentions(post)
        bindMap(post)
    }

    private fun bindAttachments(post: Post) {
        binding.attachmentContainer.isVisible = false
        binding.videoContainer.isVisible = false
        binding.audioPlay.isVisible = false
        binding.audioPlay.setOnClickListener(null)

        when (post.attachment?.type) {
            AttachmentType.IMAGE -> {
                binding.attachmentContainer.isVisible = true
                ImageLoader.loadDetailAttachmentImage(binding.attachmentImage, post.attachment.url)
                val open = View.OnClickListener {
                    findNavController().navigate(
                        R.id.action_singlePostFragment_to_photoViewFragment,
                        PhotoViewFragment.createArguments(post.attachment.url),
                    )
                }
                binding.attachmentContainer.setOnClickListener(open)
                binding.attachmentImage.setOnClickListener(open)
            }
            AttachmentType.VIDEO -> {
                binding.videoContainer.isVisible = true
                ImageLoader.loadVideoPreview(binding.videoPreview, post.attachment.url)
                val play = View.OnClickListener { playVideo(post.attachment.url) }
                binding.videoContainer.setOnClickListener(play)
                binding.playButton.setOnClickListener(play)
            }
            AttachmentType.AUDIO -> {
                binding.audioPlay.isVisible = true
                binding.audioPlay.setOnClickListener { playVideo(post.attachment.url) }
            }
            null -> {
                if (!post.video.isNullOrBlank()) {
                    binding.videoContainer.isVisible = true
                    ImageLoader.loadVideoPreview(binding.videoPreview, post.video)
                    val play = View.OnClickListener { playVideo(post.video!!) }
                    binding.videoContainer.setOnClickListener(play)
                    binding.playButton.setOnClickListener(play)
                }
            }
        }
    }

    private fun bindLikers(post: Post) {
        val likerIds = post.likeOwnerIds
        val hasLikers = likerIds.isNotEmpty()
        binding.likersTitle.isVisible = hasLikers
        binding.likersRow.isVisible = hasLikers
        if (!hasLikers) return

        binding.likersCount.text = post.likesCount.toString()
        val chips = likerIds.map { id ->
            val preview = post.users[id.toString()]
            UserAvatarUiHelper.ChipUser(id, preview?.avatar)
        }
        UserAvatarUiHelper.bindAvatars(
            container = binding.likersAvatars,
            users = chips,
            onMoreClick = if (likerIds.size > 5) {
                {
                    findNavController().navigate(
                        R.id.action_singlePostFragment_to_postLikersFragment,
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

    private fun bindMentions(post: Post) {
        val mentionIds = post.mentionIds
        val hasMentions = mentionIds.isNotEmpty()
        binding.mentionsTitle.isVisible = hasMentions
        binding.mentionsRow.isVisible = hasMentions
        if (!hasMentions) return

        binding.mentionsCount.text = mentionIds.size.toString()
        val chips = mentionIds.mapNotNull { id ->
            post.users[id.toString()]?.let { preview ->
                UserAvatarUiHelper.ChipUser(id, preview.avatar)
            }
        }
        UserAvatarUiHelper.bindAvatars(
            container = binding.mentionsAvatars,
            users = chips,
            onMoreClick = if (mentionIds.size > 5) {
                {
                    findNavController().navigate(
                        R.id.action_singlePostFragment_to_postMentionedFragment,
                        bundleOf(
                            PostLikersFragment.ARG_USER_IDS to mentionIds.toLongArray(),
                            PostLikersFragment.ARG_TITLE_RES to R.string.mentioned,
                        ),
                    )
                }
            } else {
                null
            },
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

    private fun bindMap(post: Post) {
        val coords = post.coords
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

    private fun playVideo(url: String) {
        ru.netology.nmedia.util.MediaPlaybackHelper.play(requireContext(), url)
    }

    override fun onDestroyView() {
        currentPost = null
        if (isRemoving) {
            (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
        super.onDestroyView()
        _binding = null
    }

    companion object {
        var Bundle.postId: Long by LongArg
    }
}
