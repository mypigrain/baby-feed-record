package com.example.baby.data

import android.content.Context

data class BabyProfile(
    val name: String,
    val birthDate: String
)

object BabyProfileManager {
    private const val PREFS_NAME = "baby_profile"
    private const val KEY_NAME = "baby_name"
    private const val KEY_BIRTH = "baby_birth_date"

    fun getProfile(context: Context): BabyProfile? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val birthDate = prefs.getString(KEY_BIRTH, null) ?: return null
        return BabyProfile(name, birthDate)
    }

    fun saveProfile(context: Context, name: String, birthDate: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, name)
            .putString(KEY_BIRTH, birthDate)
            .apply()
    }
}
