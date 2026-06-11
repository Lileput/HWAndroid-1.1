package ru.netology.nmedia.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.netology.nmedia.activity.UserJobsFragment
import ru.netology.nmedia.activity.UserWallFragment

class ProfilePagerAdapter(
    fragment: Fragment,
    private val userId: Long,
    private val isOwnProfile: Boolean,
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> UserWallFragment.newInstance(userId)
        else -> UserJobsFragment.newInstance(userId, isOwnProfile)
    }
}
