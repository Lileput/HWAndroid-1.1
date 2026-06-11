package ru.netology.nmedia.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.JobAdapter
import ru.netology.nmedia.adapter.OnJobInteractionListener
import ru.netology.nmedia.databinding.FragmentUserJobsBinding
import ru.netology.nmedia.dto.Job
import ru.netology.nmedia.viewModel.ProfileViewModel

@AndroidEntryPoint
class UserJobsFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() })

    private var _binding: FragmentUserJobsBinding? = null
    private val binding get() = _binding!!

    private var adapter: JobAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserJobsBinding.inflate(inflater, container, false)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        setupAdapter(viewModel.isOwnProfile)

        viewModel.jobs.observe(viewLifecycleOwner) { jobs ->
            adapter?.submitList(jobs)
            val editable = viewModel.isOwnProfile
            binding.empty.isVisible = jobs.isEmpty() && viewModel.jobsLoading.value != true
            binding.list.isVisible = jobs.isNotEmpty() || editable
        }

        viewModel.jobsLoading.observe(viewLifecycleOwner) { loading ->
            binding.progress.isVisible = loading == true && (adapter?.itemCount ?: 0) == 0
        }

        viewModel.jobsError.observe(viewLifecycleOwner) { error ->
            val hasError = !error.isNullOrBlank()
            binding.errorGroup.isVisible = hasError && (adapter?.itemCount ?: 0) == 0
            if (hasError) {
                binding.errorTitle.text = error
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.user.observe(viewLifecycleOwner) {
            setupAdapter(viewModel.isOwnProfile)
        }

        binding.retry.setOnClickListener { viewModel.loadJobs() }

        return binding.root
    }

    private fun setupAdapter(editable: Boolean) {
        adapter = JobAdapter(
            editable = editable,
            onInteractionListener = if (editable) {
                object : OnJobInteractionListener {
                    override fun onDelete(job: Job) {
                        AlertDialog.Builder(requireContext())
                            .setMessage(R.string.delete_job_confirmation)
                            .setPositiveButton(R.string.yes) { _, _ -> viewModel.removeJob(job.id) }
                            .setNegativeButton(R.string.no, null)
                            .show()
                    }
                }
            } else {
                null
            },
        )
        binding.list.adapter = adapter
        viewModel.jobs.value?.let { adapter?.submitList(it) }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isOwnProfile) {
            viewModel.loadJobs()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        _binding = null
    }

    companion object {
        private const val ARG_USER_ID = "user_id"

        fun newInstance(userId: Long, editable: Boolean): UserJobsFragment = UserJobsFragment().apply {
            arguments = bundleOf(ARG_USER_ID to userId)
        }
    }
}
