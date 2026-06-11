package ru.netology.nmedia.activity

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.DialogJobDatesBinding
import ru.netology.nmedia.util.Formatter
import java.util.Calendar
import java.util.TimeZone

class JobDatesDialogFragment : DialogFragment() {

    private var startEpoch: Long? = null
    private var finishEpoch: Long? = null

    var onApply: ((String, String?) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogJobDatesBinding.inflate(LayoutInflater.from(requireContext()))

        startEpoch = arguments?.getLong(ARG_START_EPOCH)?.takeIf { it > 0L }
        finishEpoch = arguments?.getLong(ARG_FINISH_EPOCH)?.takeIf { it > 0L }

        updateFields(binding)

        binding.startInput.setOnClickListener { pickDate(isStart = true, binding) }
        binding.startInputLayout.setEndIconOnClickListener { pickDate(isStart = true, binding) }
        binding.finishInput.setOnClickListener { pickDate(isStart = false, binding) }
        binding.finishInputLayout.setEndIconOnClickListener { pickDate(isStart = false, binding) }

        binding.cancel.setOnClickListener { dismiss() }
        binding.ok.setOnClickListener {
            val start = startEpoch
            if (start == null) {
                binding.startInputLayout.error = getString(R.string.error_job_dates_required)
                return@setOnClickListener
            }
            onApply?.invoke(
                Formatter.isoFromEpochSeconds(start),
                finishEpoch?.let { Formatter.isoFromEpochSeconds(it) },
            )
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun pickDate(isStart: Boolean, binding: DialogJobDatesBinding) {
        val current = if (isStart) startEpoch else finishEpoch
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(
                if (isStart) getString(R.string.job_start_date) else getString(R.string.job_end_date),
            )
            .setSelection(current?.times(1000) ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { dateMillis ->
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = dateMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val epoch = cal.timeInMillis / 1000
            if (isStart) {
                startEpoch = epoch
                binding.startInputLayout.error = null
            } else {
                finishEpoch = epoch
            }
            updateFields(binding)
        }
        picker.show(parentFragmentManager, if (isStart) "job_start_date" else "job_finish_date")
    }

    private fun updateFields(binding: DialogJobDatesBinding) {
        binding.startInput.setText(
            startEpoch?.let { Formatter.formatJobShortDate(Formatter.isoFromEpochSeconds(it)) }.orEmpty(),
        )
        binding.finishInput.setText(
            finishEpoch?.let { Formatter.formatJobShortDate(Formatter.isoFromEpochSeconds(it)) }.orEmpty(),
        )
    }

    companion object {
        private const val ARG_START_EPOCH = "start_epoch"
        private const val ARG_FINISH_EPOCH = "finish_epoch"

        fun createArguments(startEpoch: Long?, finishEpoch: Long?): Bundle = bundleOf(
            ARG_START_EPOCH to (startEpoch ?: 0L),
            ARG_FINISH_EPOCH to (finishEpoch ?: 0L),
        )
    }
}
