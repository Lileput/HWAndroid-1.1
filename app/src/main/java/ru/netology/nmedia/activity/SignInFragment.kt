package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignInBinding
import ru.netology.nmedia.viewModel.SignInViewModel

class SignInFragment : Fragment() {

    private val viewModel: SignInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignInBinding.inflate(inflater, container, false)

        binding.signIn.setOnClickListener {
            val login = binding.login.text.toString()
            val pass = binding.pass.text.toString()

            if (login.isBlank() || pass.isBlank()) {
                Snackbar.make(binding.root, R.string.error_empty_fields, Snackbar.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }

            viewModel.authentication(login, pass)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progress.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.signIn.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigate(R.id.feetFragment)
            }
        }

        return binding.root
    }
}
