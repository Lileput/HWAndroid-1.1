package ru.netology.nmedia.activity


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.activity.AppActivity.Companion.textArg
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.viewModel.PostViewModel

class NewPostFragment : Fragment() {

    companion object {
        const val EXTRA_EDIT_POST = "edit_post"
        const val EXTRA_EDIT_POST_ID = "edit_post_id"
        private const val KEY_POST_PUBLISHED = "post_published"
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

        binding.ok.setOnClickListener {
            val content = binding.content.text.toString().trim()
            if (content.isNotEmpty()) {
                if (postId != 0L) {
                    viewModel.edit(postId, content)
                } else {
                    viewModel.save(content)
                    postPublished = true
                }
                AndroidUtils.hideKeyboard(requireView())
                findNavController().navigateUp()
            }
        }

        binding.content.doAfterTextChanged { text ->
            if (postId == 0L) {
                viewModel.saveDraft(text.toString())
            }
        }

        return binding.root
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
