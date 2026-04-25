package com.example.baby.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.baby.data.AppDatabase
import com.example.baby.data.FeedingRecord
import com.example.baby.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

data class WeeklySummary(
    val weekLabel: String,
    val totalMl: Int,
    val totalCount: Int
)

data class StatsUiState(
    val weeklyTotalMl: Int = 0,
    val weeklyTotalCount: Int = 0,
    val dailyAmounts: List<DailyAmount> = emptyList(),
    val typeDistribution: TypeDistribution = TypeDistribution(),
    val weeklySummaries: List<WeeklySummary> = emptyList()
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).feedingDao()

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            val weekStart = DateUtils.getWeekStart()
            val weekEnd = DateUtils.getWeekEnd()

            // Collect all records for this week
            dao.getRecordsForWeek(weekStart, weekEnd).collect { records ->
                computeStats(records, weekStart)
            }
        }
    }

    private suspend fun computeStats(weekRecords: List<FeedingRecord>, weekStart: Long) {
        val weeklyTotalMl = weekRecords.sumOf { it.amountMl ?: 0 }
        val weeklyTotalCount = weekRecords.size

        // Daily breakdown
        val dailyAmounts = (0..6).map { dayOffset ->
            val dayStart = weekStart + dayOffset * 86400000L
            val dayEnd = dayStart + 86400000L
            val dayRecords = weekRecords.filter {
                it.timestamp >= dayStart && it.timestamp < dayEnd
            }
            DailyAmount(
                dayLabel = when (dayOffset) {
                    0 -> "一"
                    1 -> "二"
                    2 -> "三"
                    3 -> "四"
                    4 -> "五"
                    5 -> "六"
                    6 -> "日"
                    else -> ""
                },
                totalMl = dayRecords.sumOf { it.amountMl ?: 0 },
                count = dayRecords.size
            )
        }

        // Type distribution for this week
        val typeDist = TypeDistribution(
            breast = weekRecords.count { it.type == "breast" },
            formula = weekRecords.count { it.type == "formula" },
            mixed = weekRecords.count { it.type == "mixed" }
        )

        // Weekly summaries (last 4 weeks)
        val weeklySummaries = (0 until 4).map { weekOffset ->
            val ws = weekStart - weekOffset * 7 * 86400000L
            val we = ws + 7 * 86400000L
            val records = dao.getRecordsForWeek(ws, we).first()
            val date = LocalDate.ofInstant(Instant.ofEpochMilli(ws), ZoneId.systemDefault())
            val weekOfYear = date.get(java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).weekOfYear())
            WeeklySummary(
                weekLabel = "第${weekOfYear}周",
                totalMl = records.sumOf { it.amountMl ?: 0 },
                totalCount = records.size
            )
        }

        _uiState.update {
            StatsUiState(
                weeklyTotalMl = weeklyTotalMl,
                weeklyTotalCount = weeklyTotalCount,
                dailyAmounts = dailyAmounts,
                typeDistribution = typeDist,
                weeklySummaries = weeklySummaries
            )
        }
    }
}
