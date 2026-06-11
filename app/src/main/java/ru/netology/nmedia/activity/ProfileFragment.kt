package ru.netology.nmedia.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.ProfilePagerAdapter
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentProfileBinding
import ru.netology.nmedia.dto.User
import ru.netology.nmedia.util.ImageLoader
import ru.netology.nmedia.viewModel.ProfileViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    @Inject
    lateinit var appAuth: AppAuth

    private val viewModel: ProfileViewModel by viewModels()

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var tabMediator: TabLayoutMediator? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var isOwnProfile = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        val userId = arguments?.getLong("userId") ?: 0L
        isOwnProfile = viewModel.isOwnProfile || (
            userId != 0L && userId == appAuth.authState.value?.resolvedId()
        )
        binding.pager.adapter = ProfilePagerAdapter(this, userId, isOwnProfile)

        tabMediator = TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.profile_wall)
                else -> getString(R.string.profile_jobs)
            }
        }.also { it.attach() }

        binding.addJob.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_newJobFragment)
        }
        binding.addJobContainer.bringToFront()
        updateAddJobVisibility(binding.pager.currentItem)

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateAddJobVisibility(position)
            }
        }.also { binding.pager.registerOnPageChangeCallback(it) }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            isOwnProfile = viewModel.isOwnProfile
            updateAddJobVisibility(binding.pager.currentItem)
            updateActionBar(user)
            if (user.avatar.isNullOrBlank()) {
                binding.avatarBanner.setImageResource(R.drawable.ic_baseline_person_24)
            } else {
                ImageLoader.loadDetailAttachmentImage(binding.avatarBanner, user.avatar)
            }
        }

        if (isOwnProfile) {
            requireActivity().addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.menu_profile, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                        when (menuItem.itemId) {
                            R.id.logout -> {
                                showLogoutConfirmation()
                                true
                            }
                            else -> false
                        }
                },
                viewLifecycleOwner,
                Lifecycle.State.RESUMED,
            )
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (findNavController().currentDestination?.id == R.id.profileFragment) {
            viewModel.user.value?.let { updateActionBar(it) }
        }
    }

    private fun updateActionBar(user: User) {
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = if (isOwnProfile) {
                getString(R.string.profile_you)
            } else {
                getString(R.string.profile_title, user.name, user.login)
            }
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_logout)
            .setMessage(R.string.confirm_logout_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                appAuth.clear()
                findNavController().navigate(
                    R.id.feetFragment,
                    null,
                    navOptions {
                        popUpTo(R.id.feetFragment) { inclusive = true }
                        launchSingleTop = true
                    },
                )
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun updateAddJobVisibility(tabIndex: Int) {
        val visible = isOwnProfile && tabIndex == JOBS_TAB_INDEX
        binding.addJobContainer.isVisible = visible
        if (visible) {
            binding.addJobContainer.bringToFront()
        }
    }

    override fun onDestroyView() {
        pageChangeCallback?.let { binding.pager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        tabMediator?.detach()
        tabMediator = null
        if (isRemoving) {
            (requireActivity() as AppCompatActivity).supportActionBar?.apply {
                title = getString(R.string.app_name)
                setDisplayHomeAsUpEnabled(false)
            }
        }
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val JOBS_TAB_INDEX = 1
    }
}
