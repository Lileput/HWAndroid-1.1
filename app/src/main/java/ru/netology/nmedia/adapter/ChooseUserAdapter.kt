package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.databinding.CardUserChooseBinding
import ru.netology.nmedia.dto.User
import ru.netology.nmedia.util.ImageLoader

class ChooseUserAdapter(
    private val isSelected: (Long) -> Boolean,
    private val onToggle: (User) -> Unit,
) : ListAdapter<User, ChooseUserViewHolder>(ChooseUserDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChooseUserViewHolder {
        val binding = CardUserChooseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChooseUserViewHolder(binding, isSelected, onToggle)
    }

    override fun onBindViewHolder(holder: ChooseUserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ChooseUserViewHolder(
    private val binding: CardUserChooseBinding,
    private val isSelected: (Long) -> Boolean,
    private val onToggle: (User) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(user: User) {
        binding.name.text = user.name
        binding.login.text = user.login
        ImageLoader.loadAvatar(binding.avatar, user.avatar, user.id)
        binding.selected.isChecked = isSelected(user.id)
        val toggle = {
            onToggle(user)
            binding.selected.isChecked = isSelected(user.id)
        }
        binding.root.setOnClickListener { toggle() }
        binding.selected.setOnClickListener { toggle() }
    }
}

private object ChooseUserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
}
