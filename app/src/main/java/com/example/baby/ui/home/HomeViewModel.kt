package com.example.baby.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.baby.data.AppDatabase
import com.example.baby.data.FeedingRecord
import com.example.baby.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val todayCount: Int = 0,
    val todayTotalMl: Int = 0,
    val lastRecord: FeedingRecord? = null,
    val selectedAmount: Int? = null,
    val selectedType: String = "breast",
    val isLoading: Boolean = false,
    val showConfirmation: Boolean = false,
    val lastConfirmationMessage: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).feedingDao()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshToday()
    }

    fun refreshToday() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayStart = DateUtils.getDayStart(now)
            val dayEnd = DateUtils.getDayEnd(now)

            val count = dao.getCountForDay(dayStart, dayEnd)
            val totalMl = dao.getTotalAmountForDay(dayStart, dayEnd) ?: 0
            val last = dao.getLastRecord()

            _uiState.update {
                it.copy(todayCount = count, todayTotalMl = totalMl, lastRecord = last)
            }
        }
    }

    fun selectAmount(amount: Int?) {
        _uiState.update { it.copy(selectedAmount = amount) }
    }

    fun selectType(type: String) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun recordFeeding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val state = _uiState.value
            val record = FeedingRecord(
                timestamp = System.currentTimeMillis(),
                amountMl = state.selectedAmount,
                type = state.selectedType
            )
            dao.insert(record)

            val message = buildConfirmationMessage(state.selectedAmount, state.selectedType)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedAmount = null,
                    showConfirmation = true,
                    lastConfirmationMessage = message
                )
            }

            refreshToday()
        }
    }

    fun clearConfirmation() {
        _uiState.update { it.copy(showConfirmation = false) }
    }

    private fun buildConfirmationMessage(amount: Int?, type: String): String {
        val typeLabel = when (type) {
            "formula" -> "配方奶"
            "mixed" -> "混合"
            else -> "母乳"
        }
        return if (amount != null) {
            "已记录：$typeLabel ${amount}ml"
        } else {
            "已记录：$typeLabel"
        }
    }
}
