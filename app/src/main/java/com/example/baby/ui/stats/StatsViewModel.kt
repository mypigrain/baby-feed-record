package com.example.baby.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.baby.data.AppDatabase
import com.example.baby.data.FeedingRecord
import com.example.baby.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DailyAmount(
    val dayLabel: String,
    val totalMl: Int,
    val count: Int
)

data class TypeDistribution(
    val breast: Int = 0,
    val formula: Int = 0,
    val mixed: Int = 0
)

data class DailySummary(
    val dayLabel: String,
    val totalMl: Int,
    val totalCount: Int
)

data class StatsUiState(
    val dailyTotalMl: Int = 0,
    val dailyCount: Int = 0,
    val dailyAmounts: List<DailyAmount> = emptyList(),
    val typeDistribution: TypeDistribution = TypeDistribution(),
    val dailySummaries: List<DailySummary> = emptyList(),
    val selectedDateMillis: Long = System.currentTimeMillis()
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).feedingDao()

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dao.getAllDesc(),
                _selectedDate
            ) { records, selectedDate ->
                Pair(records, selectedDate)
            }.collect { (allRecords, selectedDate) ->
                computeStats(allRecords, selectedDate)
            }
        }
    }

    private fun computeStats(allRecords: List<FeedingRecord>, selectedDateMillis: Long) {
        val now = System.currentTimeMillis()
        val todayStart = DateUtils.getDayStart(now)
        val todayEnd = DateUtils.getDayEnd(now)

        // Today's data (always based on actual today)
        val todayRecords = allRecords.filter {
            it.timestamp >= todayStart && it.timestamp < todayEnd
        }
        val dailyTotalMl = todayRecords.sumOf { it.amountMl ?: 0 }
        val dailyCount = todayRecords.size

        // 7-day window based on selected date (selected date + previous 6 days)
        val selectedDayStart = DateUtils.getDayStart(selectedDateMillis)
        val dailyAmounts = (6 downTo 0).map { dayOffset ->
            val dayStart = selectedDayStart - dayOffset * 86400000L
            val dayEnd = dayStart + 86400000L
            val dayRecords = allRecords.filter {
                it.timestamp >= dayStart && it.timestamp < dayEnd
            }
            DailyAmount(
                dayLabel = when {
                    DateUtils.isSameDay(dayStart, now) -> "今天"
                    DateUtils.isSameDay(dayStart, now - 86400000L) -> "昨天"
                    else -> DateUtils.formatDate(dayStart)
                },
                totalMl = dayRecords.sumOf { it.amountMl ?: 0 },
                count = dayRecords.size
            )
        }

        // Type distribution for today
        val typeDist = TypeDistribution(
            breast = todayRecords.count { it.type == "breast" },
            formula = todayRecords.count { it.type == "formula" },
            mixed = todayRecords.count { it.type == "mixed" }
        )

        // Daily summaries (based on selected date window)
        val dailySummaries = dailyAmounts.map { day ->
            DailySummary(
                dayLabel = day.dayLabel,
                totalMl = day.totalMl,
                totalCount = day.count
            )
        }

        _uiState.update {
            StatsUiState(
                dailyTotalMl = dailyTotalMl,
                dailyCount = dailyCount,
                dailyAmounts = dailyAmounts,
                typeDistribution = typeDist,
                dailySummaries = dailySummaries,
                selectedDateMillis = selectedDateMillis
            )
        }
    }

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
    }
}
