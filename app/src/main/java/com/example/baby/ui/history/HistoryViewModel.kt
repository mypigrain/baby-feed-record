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
    val deletedMessage: String? = null,
    val selectedDateMillis: Long = System.currentTimeMillis()
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).feedingDao()

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dao.getAllDesc(),
                _selectedDate
            ) { records, selectedDate ->
                Pair(records, selectedDate)
            }.collect { (records, selectedDate) ->
                val dayStart = DateUtils.getDayStart(selectedDate)
                val dayEnd = DateUtils.getDayEnd(selectedDate)
                val dayRecords = records.filter {
                    it.timestamp >= dayStart && it.timestamp < dayEnd
                }.sortedByDescending { it.timestamp }

                val groups = if (dayRecords.isEmpty()) {
                    emptyList()
                } else {
                    listOf(
                        HistoryGroup(
                            dayStart = dayStart,
                            dayLabel = DateUtils.getDayLabel(dayStart),
                            records = dayRecords
                        )
                    )
                }

                _uiState.update { it.copy(groups = groups, selectedDateMillis = selectedDate) }
            }
        }
    }

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
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
