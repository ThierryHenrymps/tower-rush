package com.henry.towerrush.powerups

enum class PowerUpType(val title: String, val description: String) {
    SECOND_CHANCE("Segunda Chance", "Restaura o tamanho total do bloco se falhar"),
    FREEZE("Congelamento", "Reduz drasticamente a velocidade"),
    MAGIC_BLOCK("Bloco Mágico", "Expande a área do próximo bloco")
}

class PowerUpManager {
    var hasSecondChance: Boolean = false
    var isFrozen: Boolean = false
    var freezeTicksRemaining: Int = 0
    var isMagicBlockActive: Boolean = false

    fun applyPowerUp(type: PowerUpType) {
        when (type) {
            PowerUpType.SECOND_CHANCE -> hasSecondChance = true
            PowerUpType.FREEZE -> {
                isFrozen = true
                freezeTicksRemaining = 300 // ~5 segundos a 60 FPS
            }
            PowerUpType.MAGIC_BLOCK -> isMagicBlockActive = true
        }
    }

    fun update() {
        if (isFrozen) {
            freezeTicksRemaining--
            if (freezeTicksRemaining <= 0) {
                isFrozen = false
            }
        }
    }

    fun reset() {
        hasSecondChance = false
        isFrozen = false
        freezeTicksRemaining = 0
        isMagicBlockActive = false
    }
}