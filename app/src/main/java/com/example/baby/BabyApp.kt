package com.example.baby

import android.app.Application
import androidx.room.InvalidationTracker
import com.example.baby.data.AppDatabase
import com.example.baby.widget.QuickRecordWidget

class BabyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val db = AppDatabase.getInstance(this)
        db.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer("feeding_records") {
                override fun onInvalidated(tables: Set<String>) {
                    QuickRecordWidget.refreshAllWidgets(this@BabyApp)
                }
            }
        )
    }
}
