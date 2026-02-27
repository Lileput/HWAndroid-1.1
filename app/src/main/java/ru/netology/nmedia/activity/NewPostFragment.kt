package ru.netology.nmedia.activity


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
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.AppActivity.Companion.textArg
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.viewModel.PostViewModel

class NewPostFragment : Fragment() {

    companion object {
        const val EXTRA_EDIT_POST = "edit_post"
        const val EXTRA_EDIT_POST_ID = "edit_post_id"
        private const val KEY_POST_PUBLISHED = "post_published"
        private const val MAX_SIZE_PX = 2048
    }

    private val viewModel: PostViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    private var postPublished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postPublished = savedInstanceState?.getBoolean(KEY_POST_PUBLISHED) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(inflater, container, false)

        val editPost = arguments?.getString(EXTRA_EDIT_POST)
        val postId = arguments?.getLong(EXTRA_EDIT_POST_ID, 0L) ?: 0L
        val sharedText = arguments?.textArg

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = when {
                !editPost.isNullOrBlank() -> "Редактирование"
                else -> "Новый пост"
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

            postId == 0L && !postPublished -> {
                val draft = viewModel.getDraft()
                if (!draft.isNullOrEmpty()) {
                    binding.content.setText(draft)
                    binding.content.setSelection(draft.length)
                } else {
                    binding.content.setText("")
                }
            }

            else -> {
                binding.content.setText("")
            }
        }

        val imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val uri = result.data?.data
                if (result.resultCode == ImagePicker.RESULT_ERROR) {
                    Toast.makeText(requireContext(), "Image picker error", Toast.LENGTH_LONG).show()
                } else if (uri != null) {
                    viewModel.changePhoto(uri, uri.toFile())
                }
            }

        viewModel.photo.observe(viewLifecycleOwner) { photo ->
            if (photo != null) {
                binding.previewContainer.isVisible = true
                binding.preview.setImageURI(photo.uri)
            } else {
                binding.previewContainer.isVisible = false
            }
        }

        binding.removePhoto.setOnClickListener {
            viewModel.removePhoto()
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
                            if (content.isNotEmpty()) {
                                if (postId != 0L) {
                                    viewModel.edit(postId, content)
                                } else {
                                    viewModel.saveWithCheck(content)
                                    postPublished = true
                                }
                                AndroidUtils.hideKeyboard(requireView())
                                true
                            } else {
                                false
                            }
                        }

                        else -> false
                    }
            },
            viewLifecycleOwner,
        )

        binding.takePhoto.setOnClickListener {
            ImagePicker.with(this)
                .cameraOnly()
                .crop()
                .maxResultSize(MAX_SIZE_PX, MAX_SIZE_PX)
                .createIntent(imagePickerLauncher::launch)
        }

        binding.pickPhoto.setOnClickListener {
            ImagePicker.with(this)
                .galleryOnly()
                .crop()
                .maxResultSize(MAX_SIZE_PX, MAX_SIZE_PX)
                .createIntent(imagePickerLauncher::launch)
        }

        binding.content.doAfterTextChanged { text ->
            if (postId == 0L) {
                viewModel.saveDraft(text.toString())
            }
        }

        viewModel.postCreated.observe(viewLifecycleOwner) {
            viewModel.load()
            findNavController().navigateUp()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.setHomeActionContentDescription(
            getString(R.string.navigate_up)
        )

        (requireActivity() as AppCompatActivity).supportActionBar?.setHomeButtonEnabled(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_POST_PUBLISHED, postPublished)
    }

    override fun onPause() {
        super.onPause()
        val binding = requireView().let { FragmentNewPostBinding.bind(it) }
        val content = binding.content.text.toString()
        val postId = arguments?.getLong(EXTRA_EDIT_POST_ID, 0L) ?: 0L

        if (postPublished) {
            viewModel.clearDraft()
            postPublished = false
        } else if (postId == 0L && content.isNotEmpty()) {
            viewModel.saveDraft(content)
        }
    }
}
