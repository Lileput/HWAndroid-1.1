package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.netology.nmedia.databinding.FragmentPhotoViewBinding
import ru.netology.nmedia.util.ImageLoader

class PhotoViewFragment : Fragment() {

    companion object {
        private const val ARG_IMAGE_URL = "image_url"

        fun createArguments(imageUrl: String): Bundle {
            return Bundle().apply {
                putString(ARG_IMAGE_URL, imageUrl)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPhotoViewBinding.inflate(inflater, container, false)

        val imageUrl = arguments?.getString(ARG_IMAGE_URL) ?: ""

        ImageLoader.loadFullSizeImage(binding.imageView, imageUrl)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.imageView.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }
}