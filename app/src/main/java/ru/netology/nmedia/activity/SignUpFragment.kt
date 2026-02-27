package ru.netology.nmedia.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignUpBinding
import ru.netology.nmedia.viewModel.SignUpViewModel

class SignUpFragment : Fragment() {

    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignUpBinding.inflate(inflater, container, false)

        binding.signUp.setOnClickListener {
            val name = binding.name.text.toString()
            val login = binding.login.text.toString()
            val pass = binding.pass.text.toString()
            val confirmPass = binding.confirmPass.text.toString()

            when {
                name.isBlank() || login.isBlank() || pass.isBlank() || confirmPass.isBlank() -> {
                    Snackbar.make(binding.root, R.string.error_empty_fields, Snackbar.LENGTH_LONG).show()
                }
                pass != confirmPass -> {
                    Snackbar.make(binding.root, R.string.error_passwords_not_match, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    viewModel.registration(login, pass, name)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progress.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.signUp.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, "Регистрация успешна! Загружаем данные...", Snackbar.LENGTH_LONG).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    findNavController().navigateUp()
                }, 5000)
            }
        }

        return binding.root
    }
}