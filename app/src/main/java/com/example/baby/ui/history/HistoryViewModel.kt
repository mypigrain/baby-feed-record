package com.example.baby.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.baby.data.AppDatabase
import com.example.baby.data.DeletedSyncId
import com.example.baby.data.FeedingRecord
import com.example.baby.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryGroup(
    val dayStart: Long,
    val dayLabel: String,
    val records: List<FeedingRecord>
)

data class HistoryUiState(
    val groups: List<HistoryGroup> = emptyList(),
    val deletedMessage: String? = null
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).feedingDao()

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getAllDesc().collect { records ->
                val groups = records
                    .groupBy { DateUtils.getDayStart(it.timestamp) }
                    .map { (dayStart, dayRecords) ->
                        HistoryGroup(
                            dayStart = dayStart,
                            dayLabel = DateUtils.getDayLabel(dayStart),
                            records = dayRecords
                        )
                    }
                    .sortedByDescending { it.dayStart }

                _uiState.update { it.copy(groups = groups) }
            }
        }
    }

    fun deleteRecord(record: FeedingRecord) {
        viewModelScope.launch {
            // Track the syncId so sync can propagate the deletion
            if (record.syncId != null) {
                dao.insertDeletedSyncId(DeletedSyncId(syncId = record.syncId))
            }
            dao.deleteById(record.id)
            _uiState.update { it.copy(deletedMessage = "已删除一条记录") }
        }
    }

    fun clearDeletedMessage() {
        _uiState.update { it.copy(deletedMessage = null) }
    }
}
