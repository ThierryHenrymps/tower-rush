package com.henry.towerrush.data

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tower_rush_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_COINS = "coins"
        private const val KEY_SELECTED_THEME = "selected_theme"
        private const val KEY_UNLOCKED_THEMES = "unlocked_themes"
    }

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) {
            if (value > highScore) prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()
        }

    var coins: Int
        get() = prefs.getInt(KEY_COINS, 0)
        set(value) = prefs.edit().putInt(KEY_COINS, value).apply()

    var selectedThemeId: String
        get() = prefs.getString(KEY_SELECTED_THEME, "aurora") ?: "aurora"
        set(value) = prefs.edit().putString(KEY_SELECTED_THEME, value).apply()

    fun getUnlockedThemes(): Set<String> {
        return prefs.getStringSet(KEY_UNLOCKED_THEMES, setOf("candy", "aurora")) ?: setOf("candy", "aurora")
    }

    fun unlockTheme(themeId: String) {
        val updated = getUnlockedThemes().toMutableSet()
        updated.add(themeId)
        prefs.edit().putStringSet(KEY_UNLOCKED_THEMES, updated).apply()
    }
}