package com.henry.towerrush.store

import android.graphics.Color
import com.henry.towerrush.data.GamePreferences

data class Theme(
    val id: String,
    val name: String,
    val price: Int,
    val colors: List<Int>
)

class StoreManager(private val prefs: GamePreferences) {
    val themes = listOf(
        Theme("aurora", "Aurora", 0, listOf(
            Color.parseColor("#B7A6F6"), Color.parseColor("#A4B2FA"),
            Color.parseColor("#9AC6F7"), Color.parseColor("#C6A8F2"),
            Color.parseColor("#E3A9E6"), Color.parseColor("#F2A9CF")
        )),
        Theme("candy", "Candy", 0, listOf(
            Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"),
            Color.parseColor("#FFE66D"), Color.parseColor("#FF8E72")
        )),
        Theme("ocean", "Ocean", 150, listOf(
            Color.parseColor("#0077B6"), Color.parseColor("#00B4D8"),
            Color.parseColor("#90E0EF"), Color.parseColor("#03045E")
        )),
        Theme("sunset", "Sunset", 350, listOf(
            Color.parseColor("#F72585"), Color.parseColor("#7209B7"),
            Color.parseColor("#3A0CA3"), Color.parseColor("#4361EE")
        )),
        Theme("galaxy", "Galaxy", 700, listOf(
            Color.parseColor("#240046"), Color.parseColor("#5A189A"),
            Color.parseColor("#9D4EDD"), Color.parseColor("#E0AAFF")
        )),
        Theme("neon", "Neon", 1200, listOf(
            Color.parseColor("#39FF14"), Color.parseColor("#FF073A"),
            Color.parseColor("#0FF0FC"), Color.parseColor("#CCFF00")
        )),
        Theme("rainbow", "Rainbow", 2500, listOf(
            Color.parseColor("#FF0000"), Color.parseColor("#FF7F00"),
            Color.parseColor("#FFFF00"), Color.parseColor("#00FF00"),
            Color.parseColor("#0000FF"), Color.parseColor("#8B00FF")
        ))
    )

    fun getCurrentTheme(): Theme {
        return themes.find { it.id == prefs.selectedThemeId } ?: themes.first()
    }

    val coins: Int get() = prefs.coins

    fun isUnlocked(theme: Theme): Boolean = prefs.getUnlockedThemes().contains(theme.id)

    fun canPurchase(theme: Theme): Boolean = prefs.coins >= theme.price

    fun purchaseAndEquip(theme: Theme): Boolean {
        val unlocked = prefs.getUnlockedThemes()
        if (unlocked.contains(theme.id)) {
            prefs.selectedThemeId = theme.id
            return true
        }
        if (canPurchase(theme)) {
            prefs.coins -= theme.price
            prefs.unlockTheme(theme.id)
            prefs.selectedThemeId = theme.id
            return true
        }
        return false
    }
}
