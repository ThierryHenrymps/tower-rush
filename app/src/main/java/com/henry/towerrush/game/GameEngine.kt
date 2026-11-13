package com.henry.towerrush.game

import android.graphics.Color
import com.henry.towerrush.data.GamePreferences
import com.henry.towerrush.entities.Block
import com.henry.towerrush.entities.IsoProjection
import com.henry.towerrush.entities.Tower
import com.henry.towerrush.entities.Wizard
import com.henry.towerrush.powerups.PowerUpManager
import com.henry.towerrush.powerups.PowerUpType
import com.henry.towerrush.store.StoreManager
import com.henry.towerrush.ui.GameState
import kotlin.math.abs

class GameEngine(
    val prefs: GamePreferences,
    val storeManager: StoreManager
) {
    val tower = Tower()
    val wizard = Wizard()
    val powerUpManager = PowerUpManager()

    var state: GameState = GameState.START
    var score: Int = 0
    var screenWidth: Float = 0f
    var screenHeight: Float = 0f
    var wizardSummonCount: Int = 0
    var activeDebuffMessage: String? = null
    var hasRevivedThisGame: Boolean = false

    // Medidas escalam com a largura da tela (1080 px = referência) e com o zoom da câmera.
    private var unit = 1f
    private var speedUnit = 1f
    private var blockHeight = 64f
    private var defaultWidth = 300f
    var movingBlock: Block? = null
    private var moveSpeed = 8f
    private var movingRight = true
    private var stackCount = 0

    // Informações do último encaixe, usadas só pelos efeitos visuais.
    var placementCount: Int = 0
        private set
    var lastPlacedBlock: Block? = null
        private set
    var lastWasPerfect: Boolean = false
        private set
    var lastCutX: Float = 0f
        private set
    var lastCutWidth: Float = 0f
        private set

    fun setup(w: Float, h: Float) {
        screenWidth = w
        screenHeight = h
        if (w > 0f) {
            unit = (w / 1080f) * ZOOM
            blockHeight = 88f * unit
            defaultWidth = 300f * unit
            speedUnit = (w / 1080f) * SPEED_SCALE
            IsoProjection.configure(w, blockHeight, defaultWidth)
        }
    }

    fun startNewGame() {
        score = 0
        stackCount = 0
        moveSpeed = 8f * speedUnit
        placementCount = 0
        lastPlacedBlock = null
        wizardSummonCount = 0
        activeDebuffMessage = null
        hasRevivedThisGame = false
        powerUpManager.reset()
        wizard.reset()
        tower.clear()

        // Bloco base inicial
        val baseBlock = Block(
            x = (screenWidth - defaultWidth) / 2f - blockHeight * 0.46f,
            y = screenHeight * 0.755f,
            width = defaultWidth,
            height = blockHeight,
            color = storeManager.getCurrentTheme().colors[0]
        )
        tower.addBlock(baseBlock)
        spawnNextBlock()
        state = GameState.PLAYING
    }

    private fun spawnNextBlock() {
        val top = tower.getTopBlock() ?: return
        var nextWidth = top.width

        if (powerUpManager.isMagicBlockActive) {
            nextWidth = (nextWidth * 1.3f).coerceAtMost(screenWidth)
            powerUpManager.isMagicBlockActive = false
        }

        val theme = storeManager.getCurrentTheme()
        val color = theme.colors[stackCount % theme.colors.size]

        movingBlock = Block(
            x = 0f,
            y = top.y - blockHeight,
            width = nextWidth,
            height = blockHeight,
            color = color
        )
    }

    fun update() {
        if (state != GameState.PLAYING) return

        powerUpManager.update()

        val block = movingBlock ?: return
        val currentSpeed = if (powerUpManager.isFrozen) moveSpeed * 0.4f else moveSpeed

        if (movingRight) {
            block.x += currentSpeed
            if (block.x + block.width >= screenWidth - block.height * 0.5f) movingRight = false
        } else {
            block.x -= currentSpeed
            if (block.x <= block.height * 0.46f) movingRight = true
        }
    }

    fun onScreenTapped(touchX: Float, touchY: Float) {
        when (state) {
            GameState.START -> startNewGame()
            GameState.PLAYING -> handleBlockPlacement()
            GameState.WIZARD_SELECTION -> { /* Tratado pelo GameView via coordenadas dos cards */ }
            GameState.GAME_OVER, GameState.NEW_RECORD -> state = GameState.START
            else -> {}
        }
    }

    private fun handleBlockPlacement() {
        val current = movingBlock ?: return
        val top = tower.getTopBlock() ?: return

        val diff = current.x - top.x
        val tolerance = 8f * unit // Margem para encaixe perfeito
        val oldX = current.x
        val oldWidth = current.width
        lastWasPerfect = false
        lastCutWidth = 0f

        val coinMultiplier = if (powerUpManager.hasDoubleCoins) 2 else 1
        val scoreIncrement = if (powerUpManager.scoreMultiplierTicks > 0) 2 else 1

        if (powerUpManager.isAutoPerfectActive) {
            powerUpManager.isAutoPerfectActive = false
            current.x = top.x
            tower.addBlock(current)
            prefs.coins += 2 * coinMultiplier
            lastWasPerfect = true
        } else if (abs(diff) <= tolerance) {
            // Encaixe Perfeito
            current.x = top.x
            tower.addBlock(current)
            prefs.coins += 2 * coinMultiplier
            lastWasPerfect = true
        } else if (diff > 0 && diff < top.width) {
            // Corta lado direito
            val newWidth = top.width - diff
            current.width = newWidth
            tower.addBlock(current)
            prefs.coins += 1 * coinMultiplier
            lastCutX = current.x + newWidth
            lastCutWidth = (oldWidth - newWidth).coerceAtLeast(0f)
        } else if (diff < 0 && abs(diff) < current.width) {
            // Corta lado esquerdo
            val newWidth = current.width - abs(diff)
            lastCutX = oldX
            lastCutWidth = abs(diff)
            current.x = top.x
            current.width = newWidth
            tower.addBlock(current)
            prefs.coins += 1 * coinMultiplier
        } else {
            // Erro fatal
            if (powerUpManager.hasSecondChance) {
                powerUpManager.hasSecondChance = false
                current.x = top.x
                tower.addBlock(current)
            } else if (powerUpManager.extraShields > 0) {
                powerUpManager.extraShields--
                current.x = top.x
                tower.addBlock(current)
            } else {
                gameOver()
                return
            }
        }

        lastPlacedBlock = current
        placementCount++

        score += scoreIncrement
        stackCount++
        moveSpeed += 0.25f * speedUnit

        // Verificação de aparição do Mago a cada 10 blocos
        if (stackCount % 10 == 0) {
            wizardSummonCount++
            wizard.summon(screenWidth, screenHeight)
            if (wizardSummonCount % 2 == 0) {
                applyRandomDebuff()
            } else {
                activeDebuffMessage = null
            }
            state = GameState.WIZARD_SELECTION
            return
        }

        spawnNextBlock()
    }

    private fun applyRandomDebuff() {
        val debuffs = listOf(
            "⚠️ Maldição do Mago: Velocidade Turbinada!",
            "⚠️ Maldição do Mago: Bloco Encolhido!",
            "⚠️ Maldição do Mago: Direção Invertida!"
        )
        val choice = (0..2).random()
        activeDebuffMessage = debuffs[choice]
        when (choice) {
            0 -> moveSpeed *= 1.35f
            1 -> movingBlock?.let { it.width = (it.width * 0.75f).coerceAtLeast(80f * unit) }
            2 -> movingRight = !movingRight
        }
    }

    fun selectPowerUp(type: PowerUpType) {
        powerUpManager.applyPowerUp(type) { bonus ->
            score += bonus
        }
        activeDebuffMessage = null
        wizard.dismiss()
        state = GameState.PLAYING
        spawnNextBlock()
    }

    fun reviveGame() {
        if (hasRevivedThisGame) return
        hasRevivedThisGame = true
        spawnNextBlock()
        state = GameState.PLAYING
    }

    private fun gameOver() {
        val currentScore = score
        val oldHigh = prefs.highScore
        if (currentScore > oldHigh) {
            prefs.highScore = currentScore
            state = GameState.NEW_RECORD
        } else {
            state = GameState.GAME_OVER
        }
    }

    private companion object {
        const val ZOOM = 1.9f
        // blocos maiores => trajeto menor; a velocidade é reduzida para manter o mesmo ritmo
        const val SPEED_SCALE = 0.55f
    }
}
