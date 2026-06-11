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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentNewEventBinding
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Coordinates
import ru.netology.nmedia.dto.EventType
import ru.netology.nmedia.model.NewPostAttachment
import ru.netology.nmedia.util.AttachmentUtils
import ru.netology.nmedia.util.Formatter
import ru.netology.nmedia.viewModel.EventViewModel

@AndroidEntryPoint
class NewEventFragment : Fragment() {

    private val viewModel: EventViewModel by hiltNavGraphViewModels(R.id.nav_main)

    private var _binding: FragmentNewEventBinding? = null
    private val binding get() = _binding!!

    private var eventPublished = false

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(requireContext(), R.string.error_avatar_invalid, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            val uri = result.data?.data ?: return@registerForActivityResult
            val file = AttachmentUtils.uriToFile(requireContext(), uri) ?: return@registerForActivityResult
            if (AttachmentUtils.validateSizeOrError(requireContext(), file) != null) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventPublished = savedInstanceState?.getBoolean(KEY_EVENT_PUBLISHED) ?: false

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
        _binding = FragmentNewEventBinding.inflate(inflater, container, false)

        val editContent = arguments?.getString(EXTRA_EDIT_EVENT)
        val eventId = arguments?.getLong(EXTRA_EDIT_EVENT_ID, 0L) ?: 0L
        val isNewEvent = eventId == 0L && editContent.isNullOrBlank()

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = if (!editContent.isNullOrBlank()) {
                getString(R.string.edit_event)
            } else {
                getString(R.string.new_event)
            }
            setDisplayHomeAsUpEnabled(true)
        }

        if (!editContent.isNullOrBlank()) {
            binding.content.setText(editContent)
        }

        binding.removePhoto.setOnClickListener { viewModel.removePhoto() }
        binding.removeMedia.setOnClickListener { viewModel.removeMediaAttachment() }
        binding.takePhoto.setOnClickListener { showPhotoSourceDialog() }
        binding.pickAttachment.setOnClickListener { showAttachmentDialog() }
        binding.pickSpeakers.setOnClickListener { openChooseSpeakers() }
        binding.pickLocation.setOnClickListener {
            findNavController().navigate(
                R.id.action_newEventFragment_to_postMapFragment,
                PostMapFragment.createArguments(viewModel.coords.value),
            )
        }
        binding.pickEventDetails.setOnClickListener { showEventDetailsSheet() }

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
                            } else if (eventId != 0L) {
                                viewModel.edit(eventId, content)
                                findNavController().navigateUp()
                                true
                            } else {
                                viewModel.saveWithCheck(content)
                                eventPublished = true
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

        val eventId = arguments?.getLong(EXTRA_EDIT_EVENT_ID, 0L) ?: 0L
        if (eventId == 0L && arguments?.getString(EXTRA_EDIT_EVENT).isNullOrBlank()) {
            observeSpeakerSelection()
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

        viewModel.eventDatetimeEpoch.observe(viewLifecycleOwner) { updateEventDetailsHint() }
        viewModel.eventType.observe(viewLifecycleOwner) { updateEventDetailsHint() }

        viewModel.eventCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun updateEventDetailsHint() {
        val epoch = viewModel.eventDatetimeEpoch.value
        val type = viewModel.eventType.value ?: EventType.ONLINE
        binding.eventDetailsHint.isVisible = epoch != null
        if (epoch == null) return
        val typeLabel = when (type) {
            EventType.ONLINE -> getString(R.string.event_type_online)
            EventType.OFFLINE -> getString(R.string.event_type_offline)
        }
        binding.eventDetailsHint.text = getString(
            R.string.event_details_summary,
            typeLabel,
            Formatter.formatPostDateTime(epoch),
        )
    }

    private fun showEventDetailsSheet() {
        val sheet = EventDetailsBottomSheet()
        sheet.arguments = EventDetailsBottomSheet.createArguments(
            viewModel.eventDatetimeEpoch.value,
            viewModel.eventType.value ?: EventType.ONLINE,
        )
        sheet.onApply = { epoch, type -> viewModel.setEventDetails(epoch, type) }
        sheet.show(parentFragmentManager, "event_details")
    }

    private fun observeSpeakerSelection() {
        val handle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        handle.getLiveData<Set<Long>>(KEY_SPEAKER_IDS).observe(viewLifecycleOwner) { ids ->
            if (ids != null) {
                viewModel.setSpeakerIds(ids)
                handle.remove<Set<Long>>(KEY_SPEAKER_IDS)
            }
        }
    }

    private fun openChooseSpeakers() {
        val selected = viewModel.speakerIds.value.orEmpty()
        findNavController().navigate(
            R.id.action_newEventFragment_to_chooseUsersFragment,
            Bundle().apply {
                putLongArray(ChooseUsersFragment.ARG_SELECTED_IDS, selected.toLongArray())
                putString(ChooseUsersFragment.ARG_RESULT_KEY, KEY_SPEAKER_IDS)
            },
        )
    }

    private fun showPhotoSourceDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
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
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
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
        if (AttachmentUtils.validateSizeOrError(requireContext(), file) != null) {
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
        outState.putBoolean(KEY_EVENT_PUBLISHED, eventPublished)
    }

    override fun onPause() {
        super.onPause()
        if (eventPublished) {
            viewModel.clearNewEventState()
            eventPublished = false
        }
    }

    companion object {
        const val EXTRA_EDIT_EVENT = "edit_event"
        const val EXTRA_EDIT_EVENT_ID = "edit_event_id"
        const val KEY_SPEAKER_IDS = "speaker_ids"
        private const val KEY_EVENT_PUBLISHED = "event_published"
        private const val MAX_SIZE_PX = 2048
    }
}
