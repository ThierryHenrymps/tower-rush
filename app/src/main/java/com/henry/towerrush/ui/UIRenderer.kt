package com.henry.towerrush.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.henry.towerrush.powerups.PowerUpType
import com.henry.towerrush.store.StoreManager
import com.henry.towerrush.store.Theme
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class UIRenderer(private val storeManager: StoreManager) {

    private val blackFace: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    private val boldFace: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    private val mediumFace: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = boldFace
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF()
    private val rect2 = RectF()
    private val path = Path()

    val storeButtonBounds = RectF()
    val settingsButtonBounds = RectF()
    val botRankingButtonBounds = RectF()
    val botRankingCloseBounds = RectF()
    val powerUpCardBounds = mutableListOf<Pair<RectF, PowerUpType>>()
    val storeCardBounds = mutableListOf<Pair<RectF, Theme>>()
    val storeCloseBounds = RectF()
    private val storeRects = Array(16) { RectF() }

    private var logo: Bitmap? = null
    private var logoU = 0f
    private var glowShader: Shader? = null
    private var glowShaderW = 0f

    // ------------------------------------------------------------------ tela inicial

    fun drawStartScreen(
        canvas: Canvas, width: Float, height: Float,
        pulseAlpha: Int, highScore: Int, time: Long
    ) {
        val u = width / 941f
        ensureLogo(u)

        // Recorde (canto superior esquerdo)
        rect.set(34f * u, 40f * u, 300f * u, 142f * u)
        drawGlass(canvas, rect, 26f * u)
        drawCrownIcon(canvas, 96f * u, 66f * u, 62f * u, 46f * u, Color.WHITE)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = mediumFace
        textPaint.color = 0xFFE9DDFF.toInt()
        textPaint.textSize = 25f * u
        canvas.drawText("Recorde", 158f * u, 80f * u, textPaint)
        textPaint.typeface = boldFace
        textPaint.color = Color.WHITE
        textPaint.textSize = 46f * u
        canvas.drawText(highScore.toString(), 158f * u, 126f * u, textPaint)
        textPaint.textAlign = Paint.Align.CENTER

        // Configurações (canto superior direito)
        rect.set(800f * u, 40f * u, 906f * u, 146f * u)
        settingsButtonBounds.set(rect)
        drawGlass(canvas, rect, 28f * u)
        drawGear(canvas, rect.centerX(), rect.centerY(), 30f * u)

        // Logo
        val bob = sin(time / 900.0).toFloat() * 6f * u
        logo?.let { canvas.drawBitmap(it, (width - it.width) / 2f, 110f * u + bob, bitmapPaint) }

        // Chamada para jogar
        textPaint.typeface = boldFace
        textPaint.textSize = 62f * u
        textPaint.color = 0x66120430
        canvas.drawText("CLIQUE PARA JOGAR", width / 2f, height * 0.745f + 4f * u, textPaint)
        textPaint.color = Color.WHITE
        textPaint.alpha = pulseAlpha
        canvas.drawText("CLIQUE PARA JOGAR", width / 2f, height * 0.745f, textPaint)
        textPaint.alpha = 255

        drawStoreButton(canvas, width, height, u)
        drawBotRankingButton(canvas, width, height, u)
    }

    private fun drawBotRankingButton(canvas: Canvas, width: Float, height: Float, u: Float) {
        val cy = height * 0.95f
        botRankingButtonBounds.set(width / 2f - 240f * u, cy - 44f * u, width / 2f + 240f * u, cy + 44f * u)
        val r = 44f * u
        fillPaint.color = 0xE60F0722.toInt()
        canvas.drawRoundRect(botRankingButtonBounds, r, r, fillPaint)
        strokePaint.shader = null
        strokePaint.color = 0x55FFFFFF
        strokePaint.strokeWidth = 3f * u
        canvas.drawRoundRect(botRankingButtonBounds, r, r, strokePaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = boldFace
        textPaint.color = Color.WHITE
        textPaint.textSize = 30f * u
        canvas.drawText("🤖  RANKING DIÁRIO DE BOT", width / 2f, cy + 11f * u, textPaint)
    }

    private fun drawStoreButton(canvas: Canvas, width: Float, height: Float, u: Float) {
        val cy = height * 0.86f
        storeButtonBounds.set(width / 2f - 240f * u, cy - 70f * u, width / 2f + 240f * u, cy + 70f * u)
        val r = 70f * u
        if (glowShader == null || glowShaderW != width) {
            glowShaderW = width
            glowShader = LinearGradient(
                storeButtonBounds.left, 0f, storeButtonBounds.right, 0f,
                intArrayOf(0xFF7B3DFF.toInt(), 0xFFD07CFF.toInt(), 0xFF7B3DFF.toInt()),
                null, Shader.TileMode.CLAMP
            )
        }
        // brilho externo
        strokePaint.shader = glowShader
        strokePaint.alpha = 60
        strokePaint.strokeWidth = 16f * u
        canvas.drawRoundRect(storeButtonBounds, r, r, strokePaint)
        // corpo
        fillPaint.color = 0xE60F0722.toInt()
        canvas.drawRoundRect(storeButtonBounds, r, r, fillPaint)
        strokePaint.alpha = 255
        strokePaint.strokeWidth = 4f * u
        canvas.drawRoundRect(storeButtonBounds, r, r, strokePaint)
        strokePaint.shader = null

        // sacola
        val bx = storeButtonBounds.left + 108f * u
        rect2.set(bx - 30f * u, cy - 14f * u, bx + 30f * u, cy + 40f * u)
        fillPaint.color = Color.WHITE
        canvas.drawRoundRect(rect2, 12f * u, 12f * u, fillPaint)
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = 7f * u
        rect2.set(bx - 17f * u, cy - 42f * u, bx + 17f * u, cy - 0f * u)
        canvas.drawArc(rect2, 180f, 180f, false, strokePaint)
        fillPaint.color = 0xFF7B3DFF.toInt()
        canvas.drawCircle(bx, cy + 14f * u, 7f * u, fillPaint)

        // divisor + texto + seta
        strokePaint.color = 0x55FFFFFF
        strokePaint.strokeWidth = 3f * u
        canvas.drawLine(storeButtonBounds.left + 178f * u, cy - 34f * u, storeButtonBounds.left + 178f * u, cy + 34f * u, strokePaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = boldFace
        textPaint.color = Color.WHITE
        textPaint.textSize = 54f * u
        canvas.drawText("STORE", storeButtonBounds.left + 214f * u, cy + 19f * u, textPaint)
        textPaint.textAlign = Paint.Align.CENTER
        val ax = storeButtonBounds.right - 62f * u
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = 7f * u
        strokePaint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(ax - 8f * u, cy - 17f * u, ax + 8f * u, cy, strokePaint)
        canvas.drawLine(ax + 8f * u, cy, ax - 8f * u, cy + 17f * u, strokePaint)
        strokePaint.strokeCap = Paint.Cap.BUTT
    }

    // ------------------------------------------------------------------ HUD

    fun drawHUD(canvas: Canvas, width: Float, score: Int, coins: Int) {
        val u = width / 941f
        textPaint.typeface = blackFace
        textPaint.textSize = 120f * u
        textPaint.color = 0x55120430
        canvas.drawText(score.toString(), width / 2f, 190f * u + 5f * u, textPaint)
        textPaint.color = Color.WHITE
        canvas.drawText(score.toString(), width / 2f, 190f * u, textPaint)

        // moedas
        rect.set(width - 34f * u - 200f * u, 40f * u, width - 34f * u, 112f * u)
        drawGlass(canvas, rect, 36f * u)
        drawGem(canvas, rect.left + 42f * u, rect.centerY(), 15f * u)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = boldFace
        textPaint.textSize = 34f * u
        canvas.drawText(coins.toString(), rect.left + 70f * u, rect.centerY() + 12f * u, textPaint)
        textPaint.textAlign = Paint.Align.CENTER
    }

    // ------------------------------------------------------------------ mago / power-ups

    fun drawWizardSelection(
        canvas: Canvas, width: Float, height: Float, cards: List<PowerUpType>, alpha: Float = 1f, debuffMessage: String? = null
    ) {
        powerUpCardBounds.clear()
        if (cards.isEmpty() || alpha <= 0.01f) return
        val u = width / 941f
        canvas.saveLayerAlpha(0f, 0f, width, height, (alpha * 255f).toInt().coerceIn(0, 255))
        canvas.translate(0f, (1f - alpha) * 40f * u)

        if (debuffMessage != null) {
            rect.set(width * 0.1f, height * 0.28f, width * 0.9f, height * 0.36f)
            drawGlass(canvas, rect, 20f * u, 0xEE7B113A.toInt(), 0x88FFFFFF.toInt())
            textPaint.typeface = boldFace
            textPaint.color = Color.WHITE
            textPaint.textSize = 26f * u
            canvas.drawText(debuffMessage, width / 2f, rect.centerY() + 9f * u, textPaint)
        }

        val gap = width * 0.035f
        val n = cards.size
        val cardWidth = (width - gap * (n + 1)) / n
        val cardHeight = cardWidth * 1.35f
        val y = height * 0.42f

        cards.forEachIndexed { i, type ->
            val x = gap + i * (cardWidth + gap)
            val r = RectF(x, y, x + cardWidth, y + cardHeight)
            powerUpCardBounds.add(Pair(r, type))
            drawGlass(canvas, r, 30f * u, 0xB3180A44.toInt(), 0x55FFFFFF)

            textPaint.typeface = boldFace
            textPaint.color = Color.WHITE
            textPaint.textSize = 34f * u
            drawWrapped(canvas, type.title, r.centerX(), r.top + 72f * u, cardWidth - 40f * u, 40f * u)
            textPaint.typeface = mediumFace
            textPaint.color = 0xFFE0D2FF.toInt()
            textPaint.textSize = 26f * u
            drawWrapped(canvas, type.description, r.centerX(), r.top + 160f * u, cardWidth - 44f * u, 34f * u)
        }
        textPaint.color = Color.WHITE
        canvas.restore()
    }

    private fun drawWrapped(canvas: Canvas, text: String, cx: Float, y: Float, maxW: Float, lineH: Float) {
        var line = ""
        var yy = y
        for (word in text.split(" ")) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (textPaint.measureText(test) > maxW && line.isNotEmpty()) {
                canvas.drawText(line, cx, yy, textPaint)
                yy += lineH
                line = word
            } else {
                line = test
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line, cx, yy, textPaint)
    }

    // ------------------------------------------------------------------ game over

    fun drawGameOver(canvas: Canvas, width: Float, height: Float, score: Int, high: Int) {
        val u = width / 941f
        fillPaint.color = 0x9909051B.toInt()
        canvas.drawRect(0f, 0f, width, height, fillPaint)

        rect.set(width * 0.5f - 330f * u, height * 0.5f - 300f * u, width * 0.5f + 330f * u, height * 0.5f + 250f * u)
        drawGlass(canvas, rect, 48f * u, 0xB3180A44.toInt(), 0x55FFFFFF)

        textPaint.typeface = blackFace
        textPaint.color = Color.WHITE
        textPaint.textSize = 76f * u
        canvas.drawText("GAME OVER", width / 2f, rect.top + 130f * u, textPaint)

        textPaint.typeface = mediumFace
        textPaint.color = 0xFFE0D2FF.toInt()
        textPaint.textSize = 32f * u
        canvas.drawText("Pontos", width / 2f, rect.top + 225f * u, textPaint)
        textPaint.typeface = boldFace
        textPaint.color = Color.WHITE
        textPaint.textSize = 86f * u
        canvas.drawText(score.toString(), width / 2f, rect.top + 310f * u, textPaint)

        textPaint.typeface = mediumFace
        textPaint.color = 0xFFE0D2FF.toInt()
        textPaint.textSize = 32f * u
        canvas.drawText("Recorde  $high", width / 2f, rect.top + 390f * u, textPaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 30f * u
        canvas.drawText("Toque para reiniciar", width / 2f, rect.bottom - 40f * u, textPaint)
        textPaint.typeface = boldFace
    }

    // ------------------------------------------------------------------ loja de cores

    fun drawStore(canvas: Canvas, width: Float, height: Float, message: String?) {
        val u = width / 941f
        fillPaint.shader = null
        fillPaint.color = 0xB0090320.toInt()
        canvas.drawRect(0f, 0f, width, height, fillPaint)

        // fechar
        storeCloseBounds.set(34f * u, 50f * u, 122f * u, 138f * u)
        drawGlass(canvas, storeCloseBounds, 28f * u)
        strokePaint.shader = null
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = 6f * u
        strokePaint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(62f * u, 78f * u, 94f * u, 110f * u, strokePaint)
        canvas.drawLine(94f * u, 78f * u, 62f * u, 110f * u, strokePaint)
        strokePaint.strokeCap = Paint.Cap.BUTT

        // moedas
        rect.set(width - 34f * u - 200f * u, 62f * u, width - 34f * u, 126f * u)
        drawGlass(canvas, rect, 32f * u)
        drawGem(canvas, rect.left + 40f * u, rect.centerY(), 14f * u)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = boldFace
        textPaint.color = Color.WHITE
        textPaint.textSize = 32f * u
        canvas.drawText(storeManager.coins.toString(), rect.left + 66f * u, rect.centerY() + 11f * u, textPaint)

        // título
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = blackFace
        textPaint.textSize = 56f * u
        canvas.drawText("CORES", width / 2f, 118f * u, textPaint)
        textPaint.typeface = mediumFace
        textPaint.textSize = 26f * u
        textPaint.color = 0xFFD9C8FF.toInt()
        canvas.drawText("Escolha as cores dos blocos", width / 2f, 164f * u, textPaint)

        // grade de temas (2 colunas)
        storeCardBounds.clear()
        val themes = storeManager.themes
        val selectedId = storeManager.getCurrentTheme().id
        val margin = 34f * u
        val gap = 26f * u
        val cardW = (width - margin * 2f - gap) / 2f
        val cardH = 236f * u
        val top = 210f * u
        for (i in themes.indices) {
            if (i >= storeRects.size) break
            val theme = themes[i]
            val col = i % 2
            val row = i / 2
            val left = margin + col * (cardW + gap)
            val cardTop = top + row * (cardH + gap)
            val r = storeRects[i]
            r.set(left, cardTop, left + cardW, cardTop + cardH)
            storeCardBounds.add(Pair(r, theme))

            val unlocked = storeManager.isUnlocked(theme)
            val selected = theme.id == selectedId
            drawGlass(
                canvas, r, 30f * u,
                if (unlocked) 0xB3180A44.toInt() else 0xB30E0626.toInt(),
                0x38FFFFFF
            )
            if (selected) {
                strokePaint.shader = null
                strokePaint.color = 0xFFD07CFF.toInt()
                strokePaint.strokeWidth = 5f * u
                canvas.drawRoundRect(r, 30f * u, 30f * u, strokePaint)
            }

            // nome
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = boldFace
            textPaint.color = if (unlocked) Color.WHITE else 0xFFC9BCE6.toInt()
            textPaint.textSize = 38f * u
            canvas.drawText(theme.name, r.left + 28f * u, r.top + 62f * u, textPaint)

            // cadeado nos temas bloqueados
            if (!unlocked) drawLock(canvas, r.right - 46f * u, r.top + 46f * u, u)

            // todas as cores do tema
            val sw = 46f * u
            val sg = 11f * u
            for (c in theme.colors.indices) {
                val sx = r.left + 28f * u + c * (sw + sg)
                rect2.set(sx, r.top + 90f * u, sx + sw, r.top + 90f * u + sw)
                fillPaint.shader = null
                fillPaint.color = theme.colors[c]
                canvas.drawRoundRect(rect2, 12f * u, 12f * u, fillPaint)
                fillPaint.color = 0x40FFFFFF
                rect2.set(sx, r.top + 90f * u, sx + sw, r.top + 90f * u + sw * 0.4f)
                canvas.drawRoundRect(rect2, 12f * u, 12f * u, fillPaint)
                strokePaint.color = 0x55FFFFFF
                strokePaint.strokeWidth = 2f * u
                rect2.set(sx, r.top + 90f * u, sx + sw, r.top + 90f * u + sw)
                canvas.drawRoundRect(rect2, 12f * u, 12f * u, strokePaint)
            }

            // rodapé: estado / preço
            val fy = r.bottom - 34f * u
            if (selected) {
                strokePaint.color = 0xFF9BFFC9.toInt()
                strokePaint.strokeWidth = 5f * u
                strokePaint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(r.left + 30f * u, fy - 8f * u, r.left + 40f * u, fy + 2f * u, strokePaint)
                canvas.drawLine(r.left + 40f * u, fy + 2f * u, r.left + 58f * u, fy - 18f * u, strokePaint)
                strokePaint.strokeCap = Paint.Cap.BUTT
                textPaint.color = 0xFF9BFFC9.toInt()
                textPaint.textSize = 28f * u
                canvas.drawText("EQUIPADO", r.left + 72f * u, fy + 2f * u, textPaint)
            } else if (unlocked) {
                textPaint.typeface = mediumFace
                textPaint.color = 0xFFE0D2FF.toInt()
                textPaint.textSize = 28f * u
                canvas.drawText("Toque para usar", r.left + 28f * u, fy + 2f * u, textPaint)
            } else {
                val afford = storeManager.canPurchase(theme)
                drawGem(canvas, r.left + 42f * u, fy - 8f * u, 13f * u)
                textPaint.color = if (afford) Color.WHITE else 0xFFFF9AA8.toInt()
                textPaint.textSize = 30f * u
                canvas.drawText(theme.price.toString(), r.left + 66f * u, fy + 2f * u, textPaint)
            }
        }

        // aviso
        if (message != null) {
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = boldFace
            textPaint.textSize = 30f * u
            val tw = textPaint.measureText(message) + 80f * u
            rect.set(width / 2f - tw / 2f, height * 0.92f - 40f * u, width / 2f + tw / 2f, height * 0.92f + 40f * u)
            drawGlass(canvas, rect, 40f * u, 0xE60F0722.toInt(), 0x55FFFFFF)
            textPaint.color = Color.WHITE
            canvas.drawText(message, width / 2f, rect.centerY() + 11f * u, textPaint)
        }
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = boldFace
        textPaint.color = Color.WHITE
    }

    fun drawBotRanking(canvas: Canvas, width: Float, height: Float, highScore: Int) {
        val u = width / 941f
        fillPaint.color = 0xB0090320.toInt()
        canvas.drawRect(0f, 0f, width, height, fillPaint)

        // Botão Fechar
        botRankingCloseBounds.set(34f * u, 50f * u, 122f * u, 138f * u)
        drawGlass(canvas, botRankingCloseBounds, 28f * u)
        strokePaint.shader = null
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = 6f * u
        strokePaint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(62f * u, 78f * u, 94f * u, 110f * u, strokePaint)
        canvas.drawLine(94f * u, 78f * u, 62f * u, 110f * u, strokePaint)
        strokePaint.strokeCap = Paint.Cap.BUTT

        // Título
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = blackFace
        textPaint.textSize = 48f * u
        canvas.drawText("RANKING DIÁRIO", width / 2f, 115f * u, textPaint)
        textPaint.typeface = mediumFace
        textPaint.textSize = 24f * u
        textPaint.color = 0xFFD9C8FF.toInt()
        canvas.drawText("Atualizado há 12 minutos", width / 2f, 155f * u, textPaint)

        // Lista de ranking
        val bots = listOf(
            Triple("1. 🥇 CyberMaster", 342, false),
            Triple("2. 🥈 TowerLegend", 298, false),
            Triple("3. 🥉 DeepStack", 256, false),
            Triple("4. 👤 Você", max(highScore, 120), true),
            Triple("5. 🤖 PixelKing", 184, false),
            Triple("6. 🤖 ShadowKnight", 142, false),
            Triple("7. 🤖 QuickStack", 115, false)
        )

        val top = 200f * u
        val rowH = 88f * u
        val margin = 40f * u
        val w = width - margin * 2f

        bots.forEachIndexed { index, bot ->
            val ry = top + index * (rowH + 14f * u)
            rect.set(margin, ry, margin + w, ry + rowH)
            drawGlass(canvas, rect, 22f * u, if (bot.third) 0xCC3D1C8C.toInt() else 0xB3180A44.toInt(), if (bot.third) 0xFFFFE27A.toInt() else 0x38FFFFFF)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = if (bot.third) boldFace else mediumFace
            textPaint.color = if (bot.third) 0xFFFFE27A.toInt() else Color.WHITE
            textPaint.textSize = 30f * u
            canvas.drawText(bot.first, rect.left + 30f * u, rect.centerY() + 10f * u, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.typeface = boldFace
            canvas.drawText("${bot.second} pts", rect.right - 30f * u, rect.centerY() + 10f * u, textPaint)
        }
        textPaint.textAlign = Paint.Align.CENTER
    }

    private fun drawLock(canvas: Canvas, cx: Float, cy: Float, u: Float) {
        fillPaint.shader = null
        fillPaint.color = 0xFFC9BCE6.toInt()
        rect2.set(cx - 15f * u, cy - 4f * u, cx + 15f * u, cy + 20f * u)
        canvas.drawRoundRect(rect2, 6f * u, 6f * u, fillPaint)
        strokePaint.shader = null
        strokePaint.color = 0xFFC9BCE6.toInt()
        strokePaint.strokeWidth = 4.5f * u
        rect2.set(cx - 9f * u, cy - 20f * u, cx + 9f * u, cy + 4f * u)
        canvas.drawArc(rect2, 180f, 180f, false, strokePaint)
    }

    // ------------------------------------------------------------------ elementos

    private fun drawGlass(
        canvas: Canvas, r: RectF, radius: Float,
        fill: Int = 0x8C150A3A.toInt(), border: Int = 0x38FFFFFF
    ) {
        fillPaint.shader = null
        fillPaint.color = fill
        canvas.drawRoundRect(r, radius, radius, fillPaint)
        strokePaint.shader = null
        strokePaint.color = border
        strokePaint.strokeWidth = max(1.5f, radius * 0.05f)
        canvas.drawRoundRect(r, radius, radius, strokePaint)
    }

    private fun drawGear(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        strokePaint.shader = null
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = r * 0.36f
        canvas.drawCircle(cx, cy, r * 0.62f, strokePaint)
        strokePaint.strokeWidth = r * 0.34f
        for (i in 0 until 8) {
            val a = i * 0.7853982f
            canvas.drawLine(
                cx + cos(a) * r * 0.74f, cy + sin(a) * r * 0.74f,
                cx + cos(a) * r * 1.04f, cy + sin(a) * r * 1.04f, strokePaint
            )
        }
    }

    private fun drawGem(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        path.reset()
        path.moveTo(cx, cy - r); path.lineTo(cx + r, cy)
        path.lineTo(cx, cy + r); path.lineTo(cx - r, cy); path.close()
        fillPaint.shader = null
        fillPaint.color = 0xFF6FE3FF.toInt()
        canvas.drawPath(path, fillPaint)
        fillPaint.color = 0x66FFFFFF
        path.reset()
        path.moveTo(cx, cy - r); path.lineTo(cx - r, cy); path.lineTo(cx, cy); path.close()
        canvas.drawPath(path, fillPaint)
    }

    private fun drawCrownIcon(canvas: Canvas, cx: Float, top: Float, cw: Float, ch: Float, color: Int) {
        crownPath(path, cx, top, cw, ch)
        fillPaint.shader = null
        fillPaint.color = color
        canvas.drawPath(path, fillPaint)
    }

    private fun crownPath(p: Path, cx: Float, top: Float, cw: Float, ch: Float) {
        val l = cx - cw / 2f
        val r = cx + cw / 2f
        val b = top + ch
        p.reset()
        p.moveTo(l, b)
        p.lineTo(l, top + ch * 0.30f)
        p.lineTo(cx - cw * 0.25f, top + ch * 0.62f)
        p.lineTo(cx, top + ch * 0.10f)
        p.lineTo(cx + cw * 0.25f, top + ch * 0.62f)
        p.lineTo(r, top + ch * 0.30f)
        p.lineTo(r, b)
        p.close()
    }

    // ------------------------------------------------------------------ logo (pré-renderizado)

    private fun ensureLogo(u: Float) {
        if (logo != null && logoU == u) return
        logo?.recycle()
        logoU = u
        val lw = (640f * u).toInt().coerceAtLeast(64)
        val lh = (390f * u).toInt().coerceAtLeast(64)
        val bmp = Bitmap.createBitmap(lw, lh, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = lw / 2f

        fancyText(c, "TOWER", cx, 200f * u, 470f * u, 0xFFFFFFFF.toInt(), 0xFFE9DEFF.toInt(), 0xFFB6A4E8.toInt(), 9f * u, 0xFF1D0A45.toInt())
        fancyText(c, "RUSH", cx, 335f * u, 480f * u, 0xFFEE95FF.toInt(), 0xFF9B3BF0.toInt(), 0xFF5A1FB0.toInt(), 13f * u, 0xFF1D0A45.toInt())
        drawBigCrown(c, cx, 2f * u, 130f * u, 92f * u)
        logo = bmp
    }

    private fun fancyText(
        c: Canvas, text: String, cx: Float, baseline: Float, targetW: Float,
        top: Int, bottom: Int, extrude: Int, depth: Float, outline: Int
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.typeface = blackFace
        p.textAlign = Paint.Align.CENTER
        p.textSkewX = -0.12f
        p.textSize = 100f
        val size = 100f * targetW / p.measureText(text)
        p.textSize = size

        p.style = Paint.Style.STROKE
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = size * 0.17f
        p.color = outline
        c.drawText(text, cx, baseline + depth, p)
        c.drawText(text, cx, baseline, p)

        p.style = Paint.Style.FILL
        p.color = extrude
        var i = depth
        while (i >= 1f) {
            c.drawText(text, cx, baseline + i, p)
            i -= 1f
        }
        p.shader = LinearGradient(0f, baseline - size * 0.75f, 0f, baseline, top, bottom, Shader.TileMode.CLAMP)
        c.drawText(text, cx, baseline, p)
    }

    private fun drawBigCrown(c: Canvas, cx: Float, top: Float, cw: Float, ch: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val pth = Path()
        c.save()
        c.rotate(-6f, cx, top + ch)
        crownPath(pth, cx, top, cw, ch)

        p.style = Paint.Style.STROKE
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = cw * 0.06f
        p.color = 0xFF1D0A45.toInt()
        c.drawPath(pth, p)

        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, top, 0f, top + ch, 0xFFFFE27A.toInt(), 0xFFF2A92B.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(pth, p)
        p.shader = null

        p.color = 0xFFD98A12.toInt()
        c.drawRect(cx - cw / 2f, top + ch * 0.78f, cx + cw / 2f, top + ch, p)
        p.color = 0xFFFFE9A0.toInt()
        c.drawCircle(cx - cw / 2f, top + ch * 0.28f, cw * 0.07f, p)
        c.drawCircle(cx, top + ch * 0.08f, cw * 0.07f, p)
        c.drawCircle(cx + cw / 2f, top + ch * 0.28f, cw * 0.07f, p)
        p.color = 0xFFE5334B.toInt()
        c.drawCircle(cx, top + ch * 0.89f, cw * 0.05f, p)
        c.restore()
    }
}
