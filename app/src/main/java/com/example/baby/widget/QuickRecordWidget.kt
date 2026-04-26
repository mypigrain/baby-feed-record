package com.example.baby.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.baby.MainActivity
import com.example.baby.R
import com.example.baby.data.AppDatabase
import com.example.baby.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class QuickRecordWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_record)

            // Tap anywhere on widget to open MainActivity and auto-record
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("quick_record", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            val dao = runBlocking(Dispatchers.IO) {
                AppDatabase.getInstance(context).feedingDao()
            }

            runBlocking(Dispatchers.IO) {
                try {
                    val todayStart = DateUtils.getDayStart(System.currentTimeMillis())
                    val todayEnd = todayStart + 86400000L

                    val count = dao.getCountForDay(todayStart, todayEnd)
                    val totalMl = dao.getTotalAmountForDay(todayStart, todayEnd) ?: 0
                    views.setTextViewText(R.id.today_summary, "今日: ${count}次 ${totalMl}ml")

                    val lastRecord = dao.getLastRecord()
                    if (lastRecord != null) {
                        val timeStr = DateUtils.formatTime(lastRecord.timestamp)
                        val amountStr = lastRecord.amountMl?.let { " ${it}ml" } ?: ""
                        views.setTextViewText(
                            R.id.last_feeding,
                            "上次: $timeStr$amountStr"
                        )
                        views.setViewVisibility(R.id.last_feeding, android.view.View.VISIBLE)

                        val diff = System.currentTimeMillis() - lastRecord.timestamp
                        val totalMinutes = (diff / 60000).toInt()
                        val elapsedText = when {
                            totalMinutes < 1 -> "不到1分钟"
                            totalMinutes < 60 -> "${totalMinutes}分钟"
                            else -> "${totalMinutes / 60}小时${totalMinutes % 60}分钟"
                        }
                        views.setTextViewText(R.id.elapsed_time, "距离上次: $elapsedText")
                        views.setViewVisibility(R.id.elapsed_time, android.view.View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.last_feeding, android.view.View.GONE)
                        views.setViewVisibility(R.id.elapsed_time, android.view.View.GONE)
                    }
                } catch (_: Exception) {
                    views.setTextViewText(R.id.today_summary, "今日: -")
                    views.setViewVisibility(R.id.last_feeding, android.view.View.GONE)
                    views.setViewVisibility(R.id.elapsed_time, android.view.View.GONE)
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
