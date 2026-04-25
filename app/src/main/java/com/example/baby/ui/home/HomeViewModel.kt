package com.example.baby.ui.home

import android.app.Application
import android.content.Context
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
    private val prefs = application.getSharedPreferences("home_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        HomeUiState(
            selectedAmount = loadLastAmount(),
            selectedType = loadLastType()
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getAllDesc().collect { allRecords ->
                val now = System.currentTimeMillis()
                val dayStart = DateUtils.getDayStart(now)
                val dayEnd = DateUtils.getDayEnd(now)
                val todayRecords = allRecords.filter {
                    it.timestamp >= dayStart && it.timestamp < dayEnd
                }
                _uiState.update {
                    it.copy(
                        todayCount = todayRecords.size,
                        todayTotalMl = todayRecords.sumOf { it.amountMl ?: 0 },
                        lastRecord = allRecords.firstOrNull()
                    )
                }
            }
        }
    }

    private fun loadLastAmount(): Int? {
        val value = prefs.getInt("last_amount", -1)
        return if (value >= 0) value else null
    }

    private fun loadLastType(): String {
        return prefs.getString("last_type", "breast") ?: "breast"
    }

    fun selectAmount(amount: Int?) {
        prefs.edit().putInt("last_amount", amount ?: -1).apply()
        _uiState.update { it.copy(selectedAmount = amount) }
    }

    fun selectType(type: String) {
        prefs.edit().putString("last_type", type).apply()
        _uiState.update { it.copy(selectedType = type) }
    }

    fun recordFeeding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val state = _uiState.value
            val record = FeedingRecord(
                syncId = java.util.UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                amountMl = state.selectedAmount,
                type = state.selectedType
            )
            dao.insert(record)

            val message = buildConfirmationMessage(state.selectedAmount, state.selectedType)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    showConfirmation = true,
                    lastConfirmationMessage = message
                )
            }
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
