package ru.netology.nmedia.adapter

import android.content.Intent
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.databinding.CardJobBinding
import ru.netology.nmedia.dto.Job
import ru.netology.nmedia.util.Formatter

interface OnJobInteractionListener {
    fun onDelete(job: Job)
}

class JobAdapter(
    private val editable: Boolean = false,
    private val onInteractionListener: OnJobInteractionListener? = null,
) : ListAdapter<Job, JobViewHolder>(JobDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = CardJobBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobViewHolder(binding, editable, onInteractionListener)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class JobViewHolder(
    private val binding: CardJobBinding,
    private val editable: Boolean,
    private val onInteractionListener: OnJobInteractionListener?,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(job: Job) {
        val context = binding.root.context
        binding.company.text = job.name
        binding.position.text = job.position
        binding.period.text = Formatter.formatJobPeriod(
            start = job.start,
            finish = job.finish,
            presentLabel = context.getString(ru.netology.nmedia.R.string.job_present),
        )

        binding.delete.isVisible = editable
        if (editable) {
            binding.delete.setOnClickListener {
                onInteractionListener?.onDelete(job)
            }
        }

        val link = job.link?.takeIf { it.isNotBlank() }
        binding.link.isVisible = link != null
        if (link != null) {
            binding.link.text = link
            binding.link.paintFlags = binding.link.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.link.setOnClickListener {
                context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
            }
        }
    }
}

private object JobDiffCallback : DiffUtil.ItemCallback<Job>() {
    override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean = oldItem == newItem
}
