package ru.netology.nmedia.viewModel

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nmedia.R
import ru.netology.nmedia.dto.Job
import ru.netology.nmedia.repository.JobRepository
import ru.netology.nmedia.util.Formatter
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class NewJobViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _startDate = MutableLiveData<String?>(null)
    val startDate: LiveData<String?> = _startDate

    private val _finishDate = MutableLiveData<String?>(null)
    val finishDate: LiveData<String?> = _finishDate

    private val _jobSaved = MutableLiveData<Unit>()
    val jobSaved: LiveData<Unit> = _jobSaved

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun setDates(startIso: String, finishIso: String?) {
        _startDate.value = startIso
        _finishDate.value = finishIso?.takeIf { it.isNotBlank() }
    }

    fun periodLabel(): String = Formatter.formatJobInputPeriod(
        startIso = _startDate.value,
        finishIso = _finishDate.value,
        presentLabel = appContext.getString(R.string.job_present),
    )

    fun save(name: String, position: String, link: String?) {
        viewModelScope.launch {
            try {
                val company = name.trim()
                val title = position.trim()
                if (company.isEmpty() || title.isEmpty()) {
                    _errorMessage.value = appContext.getString(R.string.error_empty_job_fields)
                    return@launch
                }
                val start = _startDate.value
                if (start.isNullOrBlank()) {
                    _errorMessage.value = appContext.getString(R.string.error_job_dates_required)
                    return@launch
                }

                jobRepository.save(
                    Job(
                        id = 0L,
                        name = company,
                        position = title,
                        start = start,
                        finish = _finishDate.value,
                        link = link?.trim()?.takeIf { it.isNotEmpty() },
                    ),
                )
                _jobSaved.value = Unit
            } catch (e: Exception) {
                handleError(e, R.string.error_save_job)
            }
        }
    }

    private fun handleError(e: Throwable, @StringRes defaultMessageRes: Int) {
        val defaultMessage = appContext.getString(defaultMessageRes)
        val message = when (e) {
            is HttpException -> appContext.getString(R.string.error_with_code, e.code())
            is IOException -> appContext.getString(R.string.network_error)
            else -> e.message ?: defaultMessage
        }
        _errorMessage.value = message
    }
}
