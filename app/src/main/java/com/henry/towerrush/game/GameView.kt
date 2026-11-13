package com.henry.towerrush.game

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.henry.towerrush.ads.RewardedAdManager
import com.henry.towerrush.data.BotRankingManager
import com.henry.towerrush.data.GamePreferences
import com.henry.towerrush.entities.Block
import com.henry.towerrush.entities.IsoProjection
import com.henry.towerrush.entities.Tower
import com.henry.towerrush.media.SoundManager
import com.henry.towerrush.store.StoreManager
import com.henry.towerrush.ui.BackgroundRenderer
import com.henry.towerrush.ui.EffectsRenderer
import com.henry.towerrush.ui.GameState
import com.henry.towerrush.ui.UIRenderer
import kotlin.math.abs
import kotlin.math.sin

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var isRunning = false
    private val prefs = GamePreferences(context)
    private val storeManager = StoreManager(prefs)
    private val engine = GameEngine(prefs, storeManager)
    private val uiRenderer = UIRenderer(storeManager)
    private val backgroundRenderer = BackgroundRenderer()
    private val effects = EffectsRenderer()
    private val soundManager = SoundManager(context)
    private val botRankingManager = BotRankingManager(context)
    private val rewardedAdManager = RewardedAdManager(context)

    private var pulseAlpha = 255
    private var pulseAscending = false
    private var frameTime = 0L
    private var startTimeMs = 0L
    private var lastState = GameState.START
    private var matchRecorded = false

    // Câmera suave: segue a altura da torre com easing.
    private var cameraY = 0f
    private var gameBlend = 0f
    private var storeMessage: String? = null
    private var storeMessageUntil = 0L
    private var seenPlacements = 0

    // Torre decorativa da tela inicial (não participa da lógica do jogo).
    private val showcaseTower = Tower()
    private var showcaseKey = ""
    private var showcaseTopBaseX = 0f

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        engine.setup(width.toFloat(), height.toFloat())
        startTimeMs = SystemClock.uptimeMillis()
        soundManager.startBackgroundMusic()
        isRunning = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        engine.setup(w.toFloat(), h.toFloat())
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        soundManager.release()
        soundManager.stopNewRecord()
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            // ignora
        }
    }

    override fun run() {
        while (isRunning) {
            val t0 = SystemClock.uptimeMillis()
            val canvas: Canvas? = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) holder.lockHardwareCanvas()
                else holder.lockCanvas()
            } catch (e: Exception) {
                null
            }
            if (canvas != null) {
                try {
                    update()
                    render(canvas)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            val wait = 16L - (SystemClock.uptimeMillis() - t0)
            if (wait > 0L) {
                try {
                    Thread.sleep(wait)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun update() {
        engine.update()
        frameTime = SystemClock.uptimeMillis() - startTimeMs

        if (engine.state == GameState.PLAYING) {
            matchRecorded = false
        } else if ((engine.state == GameState.GAME_OVER || engine.state == GameState.NEW_RECORD) && !matchRecorded) {
            matchRecorded = true
            botRankingManager.recordMatch(engine.score, startTimeMs + frameTime)
        }

        if (engine.state != lastState) {
            if (engine.state == GameState.NEW_RECORD) {
                soundManager.playNewRecord()
            } else if (engine.state == GameState.WIZARD_SELECTION && lastState != GameState.WIZARD_SELECTION) {
                soundManager.playMagicEvent()
            } else if (lastState == GameState.NEW_RECORD && engine.state == GameState.START) {
                soundManager.startBackgroundMusic()
            }
            lastState = engine.state
        }

        if (pulseAscending) {
            pulseAlpha += 5
            if (pulseAlpha >= 255) pulseAscending = false
        } else {
            pulseAlpha -= 5
            if (pulseAlpha <= 120) pulseAscending = true
        }

        val count = engine.placementCount
        if (count < seenPlacements) {
            // novo jogo
            seenPlacements = count
            effects.clear()
            cameraY = 0f
        } else if (count != seenPlacements) {
            seenPlacements = count
            onBlockPlaced()
        }
        effects.update()
        engine.wizard.update()

        // transição suave entre o mundo roxo do menu e o degradê da partida
        val blendTarget = if (engine.state == GameState.START || engine.state == GameState.STORE || engine.state == GameState.BOT_RANKING || engine.state == GameState.NEW_RECORD) 0f else 1f
        gameBlend += (blendTarget - gameBlend) * 0.07f
        if (abs(blendTarget - gameBlend) < 0.002f) gameBlend = blendTarget

        val top = engine.tower.getTopBlock()
        val following = engine.state == GameState.PLAYING ||
                engine.state == GameState.WIZARD_SELECTION || engine.state == GameState.GAME_OVER
        val target = if (following && top != null) (height * 0.43f - top.y).coerceAtLeast(0f) else 0f
        cameraY += (target - cameraY) * 0.10f
        if (abs(target - cameraY) < 0.1f) cameraY = target
    }

    private fun onBlockPlaced() {
        val placed = engine.lastPlacedBlock ?: return
        placed.landedAtMs = SystemClock.uptimeMillis()
        soundManager.playBlockPlace()
        if (engine.lastWasPerfect) {
            effects.spawnPerfect(placed)
        } else if (engine.lastCutWidth > 1f) {
            effects.spawnDebris(engine.lastCutX, engine.lastCutWidth, placed)
        }
    }

    private fun render(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val wizard = engine.wizard

        backgroundRenderer.draw(canvas, w, h, frameTime, cameraY, gameBlend)

        when (engine.state) {
            GameState.START -> {
                drawShowcase(canvas, w, h)
                backgroundRenderer.drawForeground(canvas, frameTime, cameraY, gameBlend)
                uiRenderer.drawStartScreen(canvas, w, h, pulseAlpha, prefs.highScore, frameTime)
            }
            GameState.STORE -> {
                drawShowcase(canvas, w, h)
                backgroundRenderer.drawForeground(canvas, frameTime, cameraY, gameBlend)
                val msg = if (SystemClock.uptimeMillis() < storeMessageUntil) storeMessage else null
                uiRenderer.drawStore(canvas, w, h, msg)
            }
            GameState.BOT_RANKING -> {
                drawShowcase(canvas, w, h)
                backgroundRenderer.drawForeground(canvas, frameTime, cameraY, gameBlend)
                uiRenderer.drawBotRanking(canvas, w, h, prefs.highScore, botRankingManager)
            }
            GameState.PLAYING -> {
                drawTower(canvas, h, engine.movingBlock)
                backgroundRenderer.drawForeground(canvas, frameTime, cameraY, gameBlend)
                wizard.draw(canvas)
                uiRenderer.drawHUD(canvas, w, engine.score, prefs.coins)
            }
            GameState.WIZARD_SELECTION -> {
                drawTower(canvas, h, null)
                wizard.draw(canvas)
                uiRenderer.drawWizardSelection(canvas, w, h, wizard.currentCards, wizard.cardsAlpha, engine.activeDebuffMessage)
            }
            GameState.GAME_OVER, GameState.NEW_RECORD -> {
                drawTower(canvas, h, null)
                backgroundRenderer.drawForeground(canvas, frameTime, cameraY, gameBlend)
                uiRenderer.drawGameOver(
                    canvas, w, h, engine.score, prefs.highScore,
                    engine.hasRevivedThisGame, rewardedAdManager.isAdLoaded(), rewardedAdManager.isLoading()
                )
            }
            else -> {}
        }
    }

    private fun drawTower(canvas: Canvas, h: Float, moving: Block?) {
        canvas.save()
        canvas.translate(0f, cameraY)
        engine.tower.drawWithMoving(canvas, moving, -cameraY, h - cameraY)
        effects.draw(canvas)
        canvas.restore()
    }

    // ------------------------------------------------------------------ torre da tela inicial

    private fun buildShowcase(w: Float, h: Float) {
        val theme = storeManager.getCurrentTheme()
        val key = "${theme.id}_${w.toInt()}_${h.toInt()}_${IsoProjection.version}"
        if (key == showcaseKey) return
        showcaseKey = key
        showcaseTower.clear()

        val ux = IsoProjection.ux
        val uy = IsoProjection.uy
        val n = 7
        val bh = h * 0.0767f
        val baseCy = h * 0.86f
        val offsets = floatArrayOf(0f, 0.03f, -0.025f, 0.04f, -0.03f, 0.05f, -0.04f)
        for (i in 0 until n) {
            val extent = w * (0.90f - 0.10f * i)            // largura horizontal na tela
            val bw = extent / (ux * 1.68f)                   // comprimento (unidades do motor)
            val dd = bw * 0.68f                              // profundidade
            val cx = w * 0.5f + offsets[i] * w
            val cy = baseCy - i * bh
            val axs = cx - (bw * ux + dd * ux) / 2f
            val ays = cy - (dd * uy - bw * uy) / 2f
            val bx = (axs - IsoProjection.ox) / ux
            val by = ays + (bx - IsoProjection.xRef) * uy
            showcaseTower.addBlock(Block(bx, by, bw, bh, theme.colors[i % theme.colors.size], dd))
        }
        showcaseTopBaseX = showcaseTower.getTopBlock()?.x ?: 0f
    }

    private fun drawShowcase(canvas: Canvas, w: Float, h: Float) {
        buildShowcase(w, h)
        showcaseTower.getTopBlock()?.let {
            it.x = showcaseTopBaseX + sin(frameTime / 900.0).toFloat() * w * 0.05f / IsoProjection.ux
        }
        val bob = sin(frameTime / 1200.0).toFloat() * w * 0.004f
        canvas.save()
        canvas.translate(0f, bob)
        showcaseTower.drawWithMoving(canvas, null, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
        canvas.restore()
    }

    // ------------------------------------------------------------------ toque

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            if (engine.state == GameState.START && uiRenderer.storeButtonBounds.contains(x, y)) {
                engine.state = GameState.STORE
                return true
            }

            if (engine.state == GameState.START && uiRenderer.botRankingButtonBounds.contains(x, y)) {
                engine.state = GameState.BOT_RANKING
                return true
            }

            if (engine.state == GameState.STORE) {
                if (uiRenderer.storeCloseBounds.contains(x, y)) {
                    engine.state = GameState.START
                    return true
                }
                for (pair in uiRenderer.storeCardBounds) {
                    if (pair.first.contains(x, y)) {
                        val theme = pair.second
                        if (!storeManager.purchaseAndEquip(theme)) {
                            storeMessage = "Moedas insuficientes"
                            storeMessageUntil = SystemClock.uptimeMillis() + 1600L
                        }
                        return true
                    }
                }
                return true
            }

            if (engine.state == GameState.BOT_RANKING) {
                if (uiRenderer.botRankingCloseBounds.contains(x, y)) {
                    engine.state = GameState.START
                    return true
                }
                return true
            }

            if (engine.state == GameState.START && uiRenderer.settingsButtonBounds.contains(x, y)) {
                return true
            }

            if (engine.state == GameState.GAME_OVER || engine.state == GameState.NEW_RECORD) {
                if (!engine.hasRevivedThisGame && uiRenderer.reviveButtonBounds.contains(x, y)) {
                    if (rewardedAdManager.isAdLoaded()) {
                        rewardedAdManager.showAd(context as Activity, onRewarded = {
                            engine.reviveGame()
                            soundManager.startBackgroundMusic()
                        }, onFailedOrClosed = {
                            storeMessage = "Anúncio fechado sem recompensa"
                            storeMessageUntil = SystemClock.uptimeMillis() + 1600L
                        })
                    } else {
                        rewardedAdManager.loadAd()
                        storeMessage = "Carregando anúncio, tente novamente"
                        storeMessageUntil = SystemClock.uptimeMillis() + 1600L
                    }
                    return true
                }
                if (uiRenderer.gameOverRestartButtonBounds.contains(x, y)) {
                    engine.state = GameState.START
                    return true
                }
                return true
            }

            if (engine.state == GameState.WIZARD_SELECTION) {
                if (engine.wizard.cardsAlpha < 0.6f) return true
                for (pair in uiRenderer.powerUpCardBounds) {
                    if (pair.first.contains(x, y)) {
                        engine.selectPowerUp(pair.second)
                        return true
                    }
                }
            }

            engine.onScreenTapped(x, y)
        }
        return true
    }
}
