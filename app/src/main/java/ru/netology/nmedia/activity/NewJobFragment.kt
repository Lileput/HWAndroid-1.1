package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentNewJobBinding
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.viewModel.NewJobViewModel
import javax.inject.Inject

@AndroidEntryPoint
class NewJobFragment : Fragment() {

    @Inject
    lateinit var appAuth: AppAuth

    private val viewModel: NewJobViewModel by viewModels()

    private var _binding: FragmentNewJobBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewJobBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.new_job)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.pickDates.setOnClickListener { showDatesDialog() }
        binding.create.setOnClickListener {
            viewModel.save(
                name = binding.nameInput.text.toString(),
                position = binding.positionInput.text.toString(),
                link = binding.linkInput.text.toString(),
            )
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (appAuth.authState.value == null) {
            findNavController().navigate(R.id.signInFragment)
            findNavController().popBackStack(R.id.newJobFragment, true)
            return
        }

        viewModel.startDate.observe(viewLifecycleOwner) { updateDatesButton() }
        viewModel.finishDate.observe(viewLifecycleOwner) { updateDatesButton() }

        viewModel.jobSaved.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun updateDatesButton() {
        val label = viewModel.periodLabel()
        binding.pickDates.text = label.ifBlank { getString(R.string.job_pick_dates) }
    }

    private fun showDatesDialog() {
        val startEpoch = viewModel.startDate.value?.let { PostEntity.publishedToEpoch(it) }
        val finishEpoch = viewModel.finishDate.value?.let { PostEntity.publishedToEpoch(it) }
        val dialog = JobDatesDialogFragment()
        dialog.arguments = JobDatesDialogFragment.createArguments(startEpoch, finishEpoch)
        dialog.onApply = { startIso, finishIso -> viewModel.setDates(startIso, finishIso) }
        dialog.show(parentFragmentManager, "job_dates")
    }

    override fun onDestroyView() {
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(false)
        }
        _binding = null
        super.onDestroyView()
    }
}
