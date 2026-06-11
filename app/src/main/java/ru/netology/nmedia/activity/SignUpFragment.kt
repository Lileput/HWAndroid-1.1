package ru.netology.nmedia.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignUpBinding
import ru.netology.nmedia.util.AttachmentUtils
import ru.netology.nmedia.util.AvatarValidator
import ru.netology.nmedia.viewModel.SignUpViewModel

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private val viewModel: SignUpViewModel by viewModels()

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private var avatarUri: Uri? = null
    private var avatarFilePath: String? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> handlePickedAvatar(result.data)
                ImagePicker.RESULT_ERROR -> {
                    val message = result.data?.let { ImagePicker.getError(it) }
                        ?: getString(R.string.error_avatar_invalid)
                    showAvatarError(message)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.login.doAfterTextChanged {
            binding.loginContainer.error = null
            updateSignUpButtonState()
        }
        binding.name.doAfterTextChanged {
            binding.nameContainer.error = null
            updateSignUpButtonState()
        }
        binding.pass.doAfterTextChanged {
            binding.passContainer.error = null
            binding.confirmPassContainer.error = null
            updateSignUpButtonState()
        }
        binding.confirmPass.doAfterTextChanged {
            binding.confirmPassContainer.error = null
            updateSignUpButtonState()
        }

        binding.avatarPicker.setOnClickListener {
            ImagePicker.with(this)
                .galleryOnly()
                .galleryMimeTypes(arrayOf("image/png", "image/jpeg", "image/jpg"))
                .cropSquare()
                .maxResultSize(AvatarValidator.MAX_SIZE_PX, AvatarValidator.MAX_SIZE_PX)
                .createIntent(imagePickerLauncher::launch)
        }

        binding.signUp.setOnClickListener {
            if (!validateFields()) return@setOnClickListener
            val name = binding.name.text.toString().trim()
            val login = binding.login.text.toString().trim()
            val pass = binding.pass.text.toString()
            val avatarFile = AttachmentUtils.uriToFile(requireContext(), avatarUri!!)
                ?: run {
                    showAvatarError(getString(R.string.error_avatar_invalid))
                    return@setOnClickListener
                }
            viewModel.registration(login, pass, name, avatarFile)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progress.visibility = if (isLoading) View.VISIBLE else View.GONE
            updateSignUpButtonState()
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

        updateSignUpButtonState()
    }

    private fun validateFields(): Boolean {
        var valid = true
        val login = binding.login.text?.toString()?.trim().orEmpty()
        val name = binding.name.text?.toString()?.trim().orEmpty()
        val pass = binding.pass.text?.toString().orEmpty()
        val confirmPass = binding.confirmPass.text?.toString().orEmpty()

        if (login.isEmpty()) {
            binding.loginContainer.error = getString(R.string.error_empty_login)
            valid = false
        } else {
            binding.loginContainer.error = null
        }

        if (name.isEmpty()) {
            binding.nameContainer.error = getString(R.string.error_empty_name)
            valid = false
        } else {
            binding.nameContainer.error = null
        }

        if (pass.isEmpty()) {
            binding.passContainer.error = getString(R.string.error_empty_password)
            valid = false
        } else {
            binding.passContainer.error = null
        }

        if (confirmPass.isEmpty()) {
            binding.confirmPassContainer.error = getString(R.string.error_empty_password)
            valid = false
        } else if (pass != confirmPass) {
            binding.confirmPassContainer.error = getString(R.string.error_passwords_not_match)
            valid = false
        } else {
            binding.confirmPassContainer.error = null
        }

        if (avatarUri == null) {
            showAvatarError(getString(R.string.error_avatar_required))
            valid = false
        } else {
            val validationError = AvatarValidator.validate(
                requireContext(),
                avatarUri!!,
                avatarFilePath,
            )
            if (validationError != null) {
                showAvatarError(validationError)
                valid = false
            } else {
                clearAvatarError()
            }
        }

        updateSignUpButtonState()
        return valid
    }

    private fun handlePickedAvatar(data: Intent?) {
        val intent = data ?: return
        val uri = intent.data ?: return
        val filePath = intent.getStringExtra(AvatarValidator.EXTRA_FILE_PATH)
        val validationError = AvatarValidator.validate(requireContext(), uri, filePath)
        if (validationError != null) {
            showAvatarError(validationError)
            return
        }
        avatarUri = uri
        avatarFilePath = filePath
        clearAvatarError()
        binding.avatarPreview.setImageURI(uri)
        binding.avatarPreview.isVisible = true
        binding.avatarIcon.isVisible = false
        updateSignUpButtonState()
    }

    private fun showAvatarError(message: String) {
        binding.avatarError.text = message
        binding.avatarError.isVisible = true
    }

    private fun clearAvatarError() {
        binding.avatarError.isVisible = false
        binding.avatarError.text = null
    }

    private fun updateSignUpButtonState() {
        val loading = viewModel.isLoading.value == true
        val login = binding.login.text?.toString()?.trim().orEmpty()
        val name = binding.name.text?.toString()?.trim().orEmpty()
        val pass = binding.pass.text?.toString().orEmpty()
        val confirmPass = binding.confirmPass.text?.toString().orEmpty()
        binding.signUp.isEnabled = !loading &&
            login.isNotEmpty() &&
            name.isNotEmpty() &&
            pass.isNotEmpty() &&
            confirmPass.isNotEmpty() &&
            pass == confirmPass &&
            avatarUri != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
