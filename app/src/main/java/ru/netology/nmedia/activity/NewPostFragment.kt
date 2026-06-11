package ru.netology.nmedia.activity

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.AppActivity.Companion.textArg
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Coordinates
import ru.netology.nmedia.model.NewPostAttachment
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.util.AttachmentUtils
import ru.netology.nmedia.viewModel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class NewPostFragment : Fragment() {

    @Inject
    lateinit var appAuth: AppAuth

    private val viewModel: PostViewModel by hiltNavGraphViewModels(R.id.nav_main)

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private var postPublished = false

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(requireContext(), R.string.error_avatar_invalid, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            val uri = result.data?.data ?: return@registerForActivityResult
            val file = AttachmentUtils.uriToFile(requireContext(), uri) ?: return@registerForActivityResult
            val sizeError = AttachmentUtils.validateSizeOrError(requireContext(), file)
            if (sizeError != null) {
                viewModel.showAttachmentTooLargeError()
                return@registerForActivityResult
            }
            viewModel.changePhoto(uri, file)
        }

    private val pickVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            handleMediaUri(uri, AttachmentType.VIDEO)
        }

    private val pickAudioLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            handleMediaUri(uri, AttachmentType.AUDIO)
        }

    companion object {
        const val EXTRA_EDIT_POST = "edit_post"
        const val EXTRA_EDIT_POST_ID = "edit_post_id"
        const val KEY_MENTION_IDS = "mention_ids"
        private const val KEY_POST_PUBLISHED = "post_published"
        private const val MAX_SIZE_PX = 2048
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postPublished = savedInstanceState?.getBoolean(KEY_POST_PUBLISHED) ?: false

        setFragmentResultListener(PostMapFragment.REQUEST_PICK_COORDS) { _, bundle ->
            viewModel.setCoords(
                Coordinates(
                    lat = bundle.getDouble(PostMapFragment.ARG_LAT),
                    long = bundle.getDouble(PostMapFragment.ARG_LNG),
                ),
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)

        val editPost = arguments?.getString(EXTRA_EDIT_POST)
        val postId = arguments?.getLong(EXTRA_EDIT_POST_ID, 0L) ?: 0L
        val sharedText = arguments?.textArg
        val isNewPost = postId == 0L && editPost.isNullOrBlank()

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = when {
                !editPost.isNullOrBlank() -> getString(R.string.edit_post)
                else -> getString(R.string.new_post)
            }
            setDisplayHomeAsUpEnabled(true)
        }

        when {
            !editPost.isNullOrBlank() -> {
                binding.content.setText(editPost)
                viewModel.clearDraft()
            }
            !sharedText.isNullOrBlank() -> {
                binding.content.setText(sharedText)
                viewModel.clearDraft()
            }
            isNewPost && !postPublished -> {
                val draft = viewModel.getDraft()
                binding.content.setText(draft.orEmpty())
                if (!draft.isNullOrEmpty()) {
                    binding.content.setSelection(draft.length)
                }
            }
            else -> binding.content.setText("")
        }

        binding.removePhoto.setOnClickListener { viewModel.removePhoto() }
        binding.removeMedia.setOnClickListener { viewModel.removeMediaAttachment() }

        binding.takePhoto.setOnClickListener { showPhotoSourceDialog() }
        binding.pickAttachment.setOnClickListener { showAttachmentDialog() }
        binding.pickMentions.setOnClickListener { openChooseUsers() }
        binding.pickLocation.setOnClickListener {
            findNavController().navigate(
                R.id.action_newPostFragment_to_postMapFragment,
                PostMapFragment.createArguments(viewModel.coords.value),
            )
        }

        binding.content.doAfterTextChanged { text ->
            if (isNewPost) {
                viewModel.saveDraft(text.toString())
            }
        }

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.new_post_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.save -> {
                            val content = binding.content.text.toString().trim()
                            if (content.isEmpty()) {
                                Snackbar.make(binding.root, R.string.error_empty_content, Snackbar.LENGTH_SHORT)
                                    .show()
                                false
                            } else {
                                if (postId != 0L) {
                                    viewModel.edit(postId, content)
                                    findNavController().navigateUp()
                                } else {
                                    viewModel.saveWithCheck(content)
                                    postPublished = true
                                }
                                AndroidUtils.hideKeyboard(requireView())
                                true
                            }
                        }
                        else -> false
                    }
            },
            viewLifecycleOwner,
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (appAuth.authState.value == null) {
            findNavController().navigate(R.id.signInFragment)
            findNavController().popBackStack(R.id.newPostFragment, true)
            return
        }

        val postId = arguments?.getLong(EXTRA_EDIT_POST_ID, 0L) ?: 0L
        if (postId == 0L && arguments?.getString(EXTRA_EDIT_POST).isNullOrBlank()) {
            observeNavigationResults()
        }

        viewModel.photo.observe(viewLifecycleOwner) { photo ->
            binding.previewContainer.isVisible = photo != null
            if (photo != null) {
                binding.preview.setImageURI(photo.uri)
            }
        }

        viewModel.mediaAttachment.observe(viewLifecycleOwner) { media ->
            binding.mediaPreviewContainer.isVisible = media != null
            if (media != null) {
                val name = media.file.name
                binding.mediaPreviewLabel.text = when (media.type) {
                    AttachmentType.AUDIO -> getString(R.string.attachment_audio, name)
                    AttachmentType.VIDEO -> getString(R.string.attachment_video, name)
                    else -> name
                }
            }
        }

        viewModel.coords.observe(viewLifecycleOwner) { coords ->
            binding.locationHint.isVisible = coords != null
            if (coords != null) {
                binding.locationHint.text = getString(
                    R.string.location_selected,
                    coords.lat,
                    coords.long,
                )
            }
        }

        viewModel.postCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        (requireActivity() as AppCompatActivity).supportActionBar?.setHomeActionContentDescription(
            getString(R.string.navigate_up),
        )
    }

    private fun observeNavigationResults() {
        val handle = findNavController().currentBackStackEntry?.savedStateHandle ?: return

        handle.getLiveData<Set<Long>>(KEY_MENTION_IDS).observe(viewLifecycleOwner) { ids ->
            if (ids != null) {
                viewModel.setMentionIds(ids)
                handle.remove<Set<Long>>(KEY_MENTION_IDS)
            }
        }
    }

    private fun openChooseUsers() {
        val selected = viewModel.mentionIds.value.orEmpty()
        findNavController().navigate(
            R.id.action_newPostFragment_to_chooseUsersFragment,
            Bundle().apply {
                putLongArray(ChooseUsersFragment.ARG_SELECTED_IDS, selected.toLongArray())
            },
        )
    }

    private fun showPhotoSourceDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pick_image_source_title)
            .setItems(
                arrayOf(
                    getString(R.string.pick_photo_camera),
                    getString(R.string.pick_photo_gallery),
                ),
            ) { _, which ->
                when (which) {
                    0 -> ImagePicker.with(this)
                        .cameraOnly()
                        .crop()
                        .maxResultSize(MAX_SIZE_PX, MAX_SIZE_PX)
                        .createIntent(imagePickerLauncher::launch)
                    1 -> ImagePicker.with(this)
                        .galleryOnly()
                        .crop()
                        .maxResultSize(MAX_SIZE_PX, MAX_SIZE_PX)
                        .createIntent(imagePickerLauncher::launch)
                }
            }
            .show()
    }

    private fun showAttachmentDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pick_attachment_title)
            .setItems(
                arrayOf(
                    getString(R.string.pick_attachment_audio),
                    getString(R.string.pick_attachment_video),
                ),
            ) { _, which ->
                when (which) {
                    0 -> pickAudioLauncher.launch("audio/*")
                    1 -> pickVideoLauncher.launch("video/*")
                }
            }
            .show()
    }

    private fun handleMediaUri(uri: Uri?, type: AttachmentType) {
        if (uri == null) return
        val file = AttachmentUtils.uriToFile(requireContext(), uri) ?: return
        val sizeError = AttachmentUtils.validateSizeOrError(requireContext(), file)
        if (sizeError != null) {
            viewModel.showAttachmentTooLargeError()
            return
        }
        viewModel.setMediaAttachment(NewPostAttachment(uri, file, type))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(false)
        }
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_POST_PUBLISHED, postPublished)
    }

    override fun onPause() {
        super.onPause()
        if (_binding == null) return
        val content = binding.content.text.toString()
        val postId = arguments?.getLong(EXTRA_EDIT_POST_ID, 0L) ?: 0L

        if (postPublished) {
            viewModel.clearNewPostState()
            postPublished = false
        } else if (postId == 0L && content.isNotEmpty()) {
            viewModel.saveDraft(content)
        }
    }
}
