package com.henry.towerrush.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import kotlin.math.max

data class Competitor(
    val name: String,
    var score: Int,
    val isPlayer: Boolean
)

class BotRankingManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bot_ranking_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_DAY = "last_day"
        private const val KEY_PROTECTION_COUNT = "protection_count"
        private const val KEY_LAST_MATCH_ID = "last_match_id"
    }

    private val botTemplates = listOf(
        Pair("CyberMaster", 1.05f),
        Pair("TowerLegend", 0.95f),
        Pair("DeepStack", 0.85f),
        Pair("PixelKing", 0.75f),
        Pair("ShadowKnight", 0.65f),
        Pair("QuickStack", 0.55f)
    )

    var protectionMatchesRemaining: Int
        get() = prefs.getInt(KEY_PROTECTION_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_PROTECTION_COUNT, max(0, value)).apply()

    private var lastMatchId: Long
        get() = prefs.getLong(KEY_LAST_MATCH_ID, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_MATCH_ID, value).apply()

    fun checkDailyReset() {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        val lastDay = prefs.getInt(KEY_LAST_DAY, 0)
        if (currentDay != lastDay) {
            prefs.edit().putInt(KEY_LAST_DAY, currentDay).apply()
            protectionMatchesRemaining = 0
            prefs.edit().remove("bot_scores_seed").apply()
        }
    }

    fun recordMatch(playerScore: Int, matchId: Long) {
        if (matchId == lastMatchId || matchId == 0L) return
        lastMatchId = matchId

        checkDailyReset()

        val competitors = getCompetitors(playerScore)
        if (competitors.first().isPlayer) {
            // Player achieved 1st place! Grant or renew protection for 3 matches.
            protectionMatchesRemaining = 3
        } else {
            // If protected, decrement counter per match concluded.
            if (protectionMatchesRemaining > 0) {
                protectionMatchesRemaining--
            }
        }
    }

    fun getCompetitors(playerHighScore: Int): List<Competitor> {
        checkDailyReset()

        val list = mutableListOf<Competitor>()
        val effectivePlayerScore = max(playerHighScore, 60)

        botTemplates.forEachIndexed { index, template ->
            val variance = 0.9f + (index * 0.025f)
            val botScore = (effectivePlayerScore * template.second * variance).toInt().coerceAtLeast(25)
            list.add(Competitor(template.first, botScore, false))
        }

        list.add(Competitor("Você", effectivePlayerScore, true))

        list.sortByDescending { it.score }

        if (protectionMatchesRemaining > 0) {
            val playerIndex = list.indexOfFirst { it.isPlayer }
            if (playerIndex > 0) {
                val firstBot = list[0]
                val playerScore = list.find { it.isPlayer }!!.score
                firstBot.score = max(10, playerScore - (4..12).random())
                list.sortByDescending { it.score }
            }
        }

        return list
    }
}
