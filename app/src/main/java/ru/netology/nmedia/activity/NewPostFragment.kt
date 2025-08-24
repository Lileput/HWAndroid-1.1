package ru.netology.nmedia.activity


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    }

    private val viewModel: PostViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

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
            !editPost.isNullOrBlank() -> binding.content.setText(editPost)
            !sharedText.isNullOrBlank() -> binding.content.setText(sharedText)
        }

        binding.ok.setOnClickListener {
            val content = binding.content.text.toString().trim()
            if (content.isNotEmpty()) {
                if (postId != 0L) {
                    viewModel.edit(postId, content)
                } else {
                    viewModel.save(content)
                }
                AndroidUtils.hideKeyboard(requireView())
                findNavController().navigateUp()
            }
        }
        return binding.root
    }
}
