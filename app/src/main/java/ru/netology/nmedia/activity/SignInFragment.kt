package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignInBinding
import ru.netology.nmedia.viewModel.SignInViewModel

@AndroidEntryPoint
class SignInFragment : Fragment() {

    private val viewModel: SignInViewModel by viewModels()

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.login.doAfterTextChanged {
            binding.loginContainer.error = null
            updateSignInButtonState()
        }
        binding.pass.doAfterTextChanged {
            binding.passContainer.error = null
            updateSignInButtonState()
        }

        binding.signIn.setOnClickListener {
            if (!validateFields()) return@setOnClickListener
            val login = binding.login.text.toString().trim()
            val pass = binding.pass.text.toString()
            viewModel.authentication(login, pass)
        }

        binding.goToSignUp.setOnClickListener {
            findNavController().navigate(R.id.signUpFragment)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progress.visibility = if (isLoading) View.VISIBLE else View.GONE
            updateSignInButtonState()
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            message ?: return@observe
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            viewModel.onToastShown()
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigateUp()
            }
        }

        updateSignInButtonState()
    }

    private fun validateFields(): Boolean {
        var valid = true
        val login = binding.login.text?.toString()?.trim().orEmpty()
        val pass = binding.pass.text?.toString().orEmpty()

        if (login.isEmpty()) {
            binding.loginContainer.error = getString(R.string.error_empty_login)
            valid = false
        } else {
            binding.loginContainer.error = null
        }

        if (pass.isEmpty()) {
            binding.passContainer.error = getString(R.string.error_empty_password)
            valid = false
        } else {
            binding.passContainer.error = null
        }

        updateSignInButtonState()
        return valid
    }

    private fun updateSignInButtonState() {
        val loading = viewModel.isLoading.value == true
        val login = binding.login.text?.toString()?.trim().orEmpty()
        val pass = binding.pass.text?.toString().orEmpty()
        binding.signIn.isEnabled = !loading && login.isNotEmpty() && pass.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
