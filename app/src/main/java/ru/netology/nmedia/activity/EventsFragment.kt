package ru.netology.nmedia.activity

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.SingleEventFragment.Companion.eventId
import ru.netology.nmedia.adapter.EventAdapter
import ru.netology.nmedia.adapter.OnEventInteractionListener
import ru.netology.nmedia.adapter.PostLoadStateAdapter
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Event
import ru.netology.nmedia.viewModel.EventViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EventsFragment : Fragment() {

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)
        val viewModel: EventViewModel by hiltNavGraphViewModels(R.id.nav_main)

        binding.newPostsBanner.isVisible = false
        binding.empty.setText(R.string.no_events)

        val adapter = EventAdapter(object : OnEventInteractionListener {
            override fun like(event: Event) {
                if (event.likedByMe) viewModel.unlikeWithCheck(event.id) else viewModel.likeWithCheck(event.id)
            }

            override fun remove(event: Event) = viewModel.removeById(event.id)

            override fun edit(event: Event) {
                findNavController().navigate(
                    R.id.action_eventsFragment_to_newEventFragment,
                    Bundle().apply {
                        putString(NewEventFragment.EXTRA_EDIT_EVENT, event.content)
                        putLong(NewEventFragment.EXTRA_EDIT_EVENT_ID, event.id)
                    },
                )
            }

            override fun share(event: Event) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, event.content)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.chooser_share_event)))
            }

            override fun participate(event: Event) = viewModel.toggleParticipationWithCheck(event.id)

            override fun onPlayVideo(videoUrl: String) {
                ru.netology.nmedia.util.MediaPlaybackHelper.play(requireContext(), videoUrl)
            }

            override fun showDeleteConfirmation(event: Event) {
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.delete_event_confirmation)
                    .setPositiveButton(R.string.yes) { _, _ -> viewModel.removeById(event.id) }
                    .setNegativeButton(R.string.no, null)
                    .show()
            }

            override fun onItemClick(event: Event) {
                findNavController().navigate(
                    R.id.action_eventsFragment_to_singleEventFragment,
                    Bundle().apply { eventId = event.id },
                )
            }

            override fun onImageClick(imageUrl: String) {
                findNavController().navigate(
                    R.id.action_eventsFragment_to_photoViewFragment,
                    PhotoViewFragment.createArguments(imageUrl),
                )
            }
        })

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PostLoadStateAdapter { adapter.retry() },
            footer = PostLoadStateAdapter { adapter.retry() },
        )

        lifecycleScope.launch {
            viewModel.data.collectLatest { adapter.submitData(it) }
        }

        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                binding.swipeRefreshLayout.isRefreshing = loadStates.refresh is LoadState.Loading
                val isEmpty = adapter.itemCount == 0
                val hasError = loadStates.refresh is LoadState.Error
                binding.empty.isVisible = isEmpty && !hasError && loadStates.refresh !is LoadState.Loading
                binding.list.isVisible = !isEmpty
                binding.progress.isVisible = loadStates.refresh is LoadState.Loading && isEmpty
                binding.errorGroup.isVisible = hasError && isEmpty
                binding.retry.isVisible = hasError && isEmpty
                binding.errorTitle.isVisible = hasError && isEmpty
                if (loadStates.refresh is LoadState.Error) {
                    val error = (loadStates.refresh as LoadState.Error).error
                    binding.errorTitle.text = error.message ?: getString(R.string.network_error)
                } else {
                    binding.errorTitle.setText(R.string.network_error)
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.ok)
                    .show()
            }
        }

        viewModel.shouldAuthenticate.observe(viewLifecycleOwner) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.auth_required)
                .setMessage(R.string.auth_required_message)
                .setPositiveButton(R.string.sign_in) { _, _ ->
                    findNavController().navigate(R.id.signInFragment)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        binding.swipeRefreshLayout.setOnRefreshListener { adapter.refresh() }
        binding.retry.setOnClickListener { adapter.retry() }

        binding.ok.setOnClickListener {
            if (appAuth.authState.value == null) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.auth_required)
                    .setMessage(R.string.auth_required_message)
                    .setPositiveButton(R.string.sign_in) { _, _ ->
                        findNavController().navigate(R.id.signInFragment)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            } else {
                findNavController().navigate(R.id.action_eventsFragment_to_newEventFragment)
            }
        }

        return binding.root
    }
}
