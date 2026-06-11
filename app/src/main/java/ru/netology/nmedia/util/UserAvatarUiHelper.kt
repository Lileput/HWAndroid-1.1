package ru.netology.nmedia.util

import android.view.LayoutInflater
import android.widget.LinearLayout
import ru.netology.nmedia.databinding.ItemAvatarMoreBinding
import ru.netology.nmedia.databinding.ItemUserAvatarChipBinding

object UserAvatarUiHelper {

    data class ChipUser(
        val id: Long,
        val avatar: String?,
    )

    private const val MAX_VISIBLE = 5
    private const val AVATAR_SIZE_DP = 40
    private const val OVERLAP_DP = -12

    fun bindAvatars(
        container: LinearLayout,
        users: List<ChipUser>,
        onMoreClick: (() -> Unit)? = null,
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        val density = container.context.resources.displayMetrics.density
        val avatarSizePx = (AVATAR_SIZE_DP * density).toInt()
        val overlapPx = (OVERLAP_DP * density).toInt()

        val visible = users.take(MAX_VISIBLE)
        val showMore = users.size > MAX_VISIBLE && onMoreClick != null

        visible.forEachIndexed { index, user ->
            val chipBinding = ItemUserAvatarChipBinding.inflate(inflater, container, false)
            ImageLoader.loadAvatar(chipBinding.avatar, user.avatar, user.id)
            val params = LinearLayout.LayoutParams(avatarSizePx, avatarSizePx)
            if (index > 0) {
                params.marginStart = overlapPx
            }
            container.addView(chipBinding.root, params)
            chipBinding.root.translationZ = index.toFloat()
            chipBinding.root.elevation = (2 + index).toFloat() * density
        }

        if (showMore) {
            val moreBinding = ItemAvatarMoreBinding.inflate(inflater, container, false)
            moreBinding.root.setOnClickListener { onMoreClick.invoke() }
            val params = LinearLayout.LayoutParams(avatarSizePx, avatarSizePx)
            if (container.childCount > 0) {
                params.marginStart = overlapPx
            }
            container.addView(moreBinding.root, params)
            moreBinding.root.translationZ = visible.size.toFloat()
            moreBinding.root.elevation = (2 + visible.size).toFloat() * density
        }
    }
}
