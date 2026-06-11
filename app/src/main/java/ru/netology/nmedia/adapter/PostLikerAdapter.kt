package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.databinding.CardUserLikerBinding
import ru.netology.nmedia.dto.User
import ru.netology.nmedia.util.ImageLoader

class PostLikerAdapter : ListAdapter<User, PostLikerViewHolder>(PostLikerDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostLikerViewHolder {
        val binding = CardUserLikerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostLikerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostLikerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PostLikerViewHolder(
    private val binding: CardUserLikerBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(user: User) {
        binding.name.text = user.name
        binding.login.text = "@${user.login}"
        ImageLoader.loadAvatar(binding.avatar, user.avatar, user.id)
    }
}

private object PostLikerDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
}
