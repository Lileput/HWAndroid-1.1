package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import ru.netology.nmedia.databinding.BottomSheetEventDetailsBinding
import ru.netology.nmedia.dto.EventType
import ru.netology.nmedia.util.Formatter
import java.util.Calendar
import java.util.TimeZone

class EventDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEventDetailsBinding? = null
    private val binding get() = _binding!!

    private var selectedEpoch: Long = System.currentTimeMillis() / 1000
    private var pendingDateMillis: Long? = null

    var onApply: ((Long, EventType) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetEventDetailsBinding.inflate(inflater, container, false)

        selectedEpoch = arguments?.getLong(ARG_DATETIME_EPOCH) ?: selectedEpoch
        val initialType = arguments?.getString(ARG_EVENT_TYPE)?.let {
            runCatching { EventType.valueOf(it) }.getOrDefault(EventType.ONLINE)
        } ?: EventType.ONLINE

        updateDateField()
        when (initialType) {
            EventType.ONLINE -> binding.typeOnline.isChecked = true
            EventType.OFFLINE -> binding.typeOffline.isChecked = true
        }

        binding.dateInput.setOnClickListener { pickDate() }
        binding.dateInputLayout.setEndIconOnClickListener { pickDate() }

        binding.apply.setOnClickListener {
            val type = if (binding.typeOffline.isChecked) EventType.OFFLINE else EventType.ONLINE
            onApply?.invoke(selectedEpoch, type)
            dismiss()
        }

        return binding.root
    }

    private fun pickDate() {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = selectedEpoch * 1000L
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(utc.timeInMillis)
            .build()
        picker.addOnPositiveButtonClickListener { dateMillis ->
            pendingDateMillis = dateMillis
            val cal = Calendar.getInstance()
            cal.timeInMillis = dateMillis
            pickTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }
        picker.show(parentFragmentManager, "event_date")
    }

    private fun pickTime(hour: Int, minute: Int) {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .build()
        timePicker.addOnPositiveButtonClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = pendingDateMillis ?: System.currentTimeMillis()
            cal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            cal.set(Calendar.MINUTE, timePicker.minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            selectedEpoch = cal.timeInMillis / 1000
            updateDateField()
        }
        timePicker.show(parentFragmentManager, "event_time")
    }

    private fun updateDateField() {
        binding.dateInput.setText(
            Formatter.formatPostDateTime(selectedEpoch),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_DATETIME_EPOCH = "datetime_epoch"
        private const val ARG_EVENT_TYPE = "event_type"

        fun createArguments(datetimeEpoch: Long?, type: EventType): Bundle = bundleOf(
            ARG_DATETIME_EPOCH to (datetimeEpoch ?: System.currentTimeMillis() / 1000),
            ARG_EVENT_TYPE to type.name,
        )
    }
}
