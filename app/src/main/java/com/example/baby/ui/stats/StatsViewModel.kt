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
    val dailySummaries: List<DailySummary> = emptyList()
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).feedingDao()

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getAllDesc().collect { allRecords ->
                val now = System.currentTimeMillis()
                val todayStart = DateUtils.getDayStart(now)
                val todayEnd = DateUtils.getDayEnd(now)
                computeStats(allRecords, todayStart, todayEnd)
            }
        }
    }

    private fun computeStats(allRecords: List<FeedingRecord>, todayStart: Long, todayEnd: Long) {
        // Today's data
        val todayRecords = allRecords.filter {
            it.timestamp >= todayStart && it.timestamp < todayEnd
        }
        val dailyTotalMl = todayRecords.sumOf { it.amountMl ?: 0 }
        val dailyCount = todayRecords.size

        // Last 7 days daily breakdown (including today)
        val dailyAmounts = (6 downTo 0).map { dayOffset ->
            val dayStart = todayStart - dayOffset * 86400000L
            val dayEnd = dayStart + 86400000L
            val dayRecords = allRecords.filter {
                it.timestamp >= dayStart && it.timestamp < dayEnd
            }
            DailyAmount(
                dayLabel = when (dayOffset) {
                    0 -> "今天"
                    1 -> "昨天"
                    else -> com.example.baby.util.DateUtils.formatDate(dayStart).substring(2)
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

        // Daily summaries (last 7 days for history list)
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
                dailySummaries = dailySummaries
            )
        }
    }
}
