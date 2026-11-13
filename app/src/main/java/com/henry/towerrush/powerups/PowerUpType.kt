package com.henry.towerrush.powerups

enum class PowerUpType(val title: String, val description: String) {
    SECOND_CHANCE("Segunda Chance", "Restaura o tamanho total do bloco se falhar"),
    FREEZE("Congelamento", "Reduz drasticamente a velocidade"),
    MAGIC_BLOCK("Bloco Mágico", "Expande a área do próximo bloco"),
    AUTO_PERFECT("Encaixe Perfeito", "O próximo bloco encaixa perfeitamente sozinho"),
    DOUBLE_COINS("Moedas Dobradas", "Dobra o ganho de moedas por bloco"),
    SCORE_BOOST("Super Pontos", "Concede 5 pontos bônus imediatamente"),
    WIDEN_BASE("Expansão Total", "Aumenta consideravelmente a largura do bloco"),
    EXTRA_LIFE("Escudo Extra", "Protege contra falhas adicionais"),
    SPEED_BOOST("Turbo Rápido", "Dobra os pontos obtidos nos próximos blocos")
}

class PowerUpManager {
    var hasSecondChance: Boolean = false
    var isFrozen: Boolean = false
    var freezeTicksRemaining: Int = 0
    var isMagicBlockActive: Boolean = false
    var isAutoPerfectActive: Boolean = false
    var hasDoubleCoins: Boolean = false
    var extraShields: Int = 0
    var scoreMultiplierTicks: Int = 0

    fun applyPowerUp(type: PowerUpType, onScoreBoost: (Int) -> Unit = {}) {
        when (type) {
            PowerUpType.SECOND_CHANCE -> hasSecondChance = true
            PowerUpType.FREEZE -> {
                isFrozen = true
                freezeTicksRemaining = 300 // ~5 segundos a 60 FPS
            }
            PowerUpType.MAGIC_BLOCK -> isMagicBlockActive = true
            PowerUpType.AUTO_PERFECT -> isAutoPerfectActive = true
            PowerUpType.DOUBLE_COINS -> hasDoubleCoins = true
            PowerUpType.SCORE_BOOST -> onScoreBoost(5)
            PowerUpType.WIDEN_BASE -> isMagicBlockActive = true
            PowerUpType.EXTRA_LIFE -> extraShields++
            PowerUpType.SPEED_BOOST -> scoreMultiplierTicks = 300
        }
    }

    fun update() {
        if (isFrozen) {
            freezeTicksRemaining--
            if (freezeTicksRemaining <= 0) {
                isFrozen = false
            }
        }
        if (scoreMultiplierTicks > 0) {
            scoreMultiplierTicks--
        }
    }

    fun reset() {
        hasSecondChance = false
        isFrozen = false
        freezeTicksRemaining = 0
        isMagicBlockActive = false
        isAutoPerfectActive = false
        hasDoubleCoins = false
        extraShields = 0
        scoreMultiplierTicks = 0
    }
}
