package ru.netology.nmedia.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formatter {
    fun formatPostDateTime(epochSeconds: Long): String {
        if (epochSeconds == 0L) return ""
        val outputFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
        return outputFormat.format(Date(epochSeconds * 1000))
    }

    fun formatDateTime(isoDate: String): String =
        formatPostDateTime(ru.netology.nmedia.entity.PostEntity.publishedToEpoch(isoDate))

    fun formatDate(dateStr: String?, presentLabel: String): String {
        if (dateStr.isNullOrBlank()) return presentLabel
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            val date = inputFormat.parse(dateStr)
            date?.let { outputFormat.format(it) } ?: presentLabel
        } catch (e: Exception) {
            dateStr ?: ""
        }
    }

    fun formatJobDate(isoDate: String): String {
        val epoch = ru.netology.nmedia.entity.PostEntity.publishedToEpoch(isoDate)
        if (epoch == 0L) return isoDate
        val outputFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        return outputFormat.format(Date(epoch * 1000))
    }

    fun formatJobPeriod(start: String, finish: String?, presentLabel: String): String {
        val startFormatted = formatJobDate(start)
        val endFormatted = finish?.takeIf { it.isNotBlank() }?.let { formatJobDate(it) } ?: presentLabel
        return "$startFormatted – $endFormatted"
    }

    fun formatJobShortDate(isoDate: String): String {
        val epoch = ru.netology.nmedia.entity.PostEntity.publishedToEpoch(isoDate)
        if (epoch == 0L) return isoDate
        val outputFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        return outputFormat.format(Date(epoch * 1000))
    }

    fun formatJobInputPeriod(startIso: String?, finishIso: String?, presentLabel: String): String {
        if (startIso.isNullOrBlank()) return ""
        val start = formatJobShortDate(startIso)
        val end = finishIso?.takeIf { it.isNotBlank() }?.let { formatJobShortDate(it) } ?: presentLabel
        return "$start – $end"
    }

    fun isoFromEpochSeconds(epoch: Long): String =
        ru.netology.nmedia.entity.PostEntity.epochToPublished(epoch)
}