package com.zeroboat.timerkit.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RunningHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RunningRepository(application)

    val records = repository.allRecords.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
