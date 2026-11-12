package com.henry.towerrush.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.henry.towerrush.powerups.PowerUpType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Mago fofo desenhado em Canvas. Aparece com uma explosão de magia, fica flutuando
 * enquanto o jogador escolhe o power-up e sai girando em uma nuvem de faíscas.
 * Coordenadas locais: pés em (0,0), cabeça para cima (y negativo), altura ~355.
 */
class Wizard {
    enum class Phase { HIDDEN, APPEARING, IDLE, LEAVING }

    var isActive: Boolean = false
    var x: Float = 0f
    var y: Float = 0f          // posição dos pés
    var scale: Float = 1f
    var currentCards: List<PowerUpType> = emptyList()
    var phase: Phase = Phase.HIDDEN
        private set

    val isVisible: Boolean get() = phase != Phase.HIDDEN

    /** 0..1: entrada das cartas (começa quando o mago já apareceu). */
    val cardsAlpha: Float
        get() = when {
            !isActive -> 0f
            phase == Phase.APPEARING -> ((t - 0.55f) / 0.35f).coerceIn(0f, 1f)
            phase == Phase.IDLE -> 1f
            else -> 0f
        }

    private var t = 0f        // segundos na fase atual
    private var clock = 0f    // segundos desde sempre (animações contínuas)

    // partículas de magia
    private val pCount = 70
    private val px = FloatArray(pCount)
    private val py = FloatArray(pCount)
    private val pvx = FloatArray(pCount)
    private val pvy = FloatArray(pCount)
    private val plife = FloatArray(pCount)
    private val psize = FloatArray(pCount)
    private val pcol = IntArray(pCount)
    private val sparkColors = intArrayOf(
        0xFFFFE27A.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF9BE0.toInt(), 0xFFA8EEFF.toInt(), 0xFFD1A8FF.toInt()
    )

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val rect = RectF()

    fun summon(screenWidth: Float, screenHeight: Float) {
        isActive = true
        x = screenWidth / 2f
        y = screenHeight * 0.40f
        scale = screenWidth / 1080f * 1.15f
        currentCards = PowerUpType.values().toList().shuffled().take(3)
        phase = Phase.APPEARING
        t = 0f
        burst(x, y - 170f * scale, 36, 9f * scale)
    }

    fun dismiss() {
        if (phase == Phase.HIDDEN) {
            isActive = false
            currentCards = emptyList()
            return
        }
        isActive = false
        currentCards = emptyList()
        phase = Phase.LEAVING
        t = 0f
        burst(x, y - 170f * scale, 42, 11f * scale)
    }

    fun reset() {
        isActive = false
        currentCards = emptyList()
        phase = Phase.HIDDEN
        t = 0f
        plife.fill(0f)
    }

    private fun burst(cx: Float, cy: Float, count: Int, speed: Float) {
        var spawned = 0
        for (i in 0 until pCount) {
            if (spawned >= count) break
            if (plife[i] > 0f) continue
            val a = Random.nextFloat() * (2f * PI.toFloat())
            val v = speed * (0.35f + Random.nextFloat() * 0.65f)
            px[i] = cx; py[i] = cy
            pvx[i] = cos(a) * v
            pvy[i] = sin(a) * v - speed * 0.15f
            plife[i] = 0.7f + Random.nextFloat() * 0.3f
            psize[i] = (7f + Random.nextFloat() * 13f) * scale
            pcol[i] = sparkColors[Random.nextInt(sparkColors.size)]
            spawned++
        }
    }

    /** Chamado uma vez por frame (~60 fps). */
    fun update() {
        val dt = 1f / 60f
        clock += dt
        if (phase != Phase.HIDDEN) {
            t += dt
            if (phase == Phase.APPEARING && t >= APPEAR_TIME) { phase = Phase.IDLE; t = 0f }
            else if (phase == Phase.LEAVING && t >= LEAVE_TIME) { phase = Phase.HIDDEN; t = 0f }
        }
        if (phase == Phase.IDLE && Random.nextInt(14) == 0) {
            // faíscas suaves saindo da varinha
            burst(x + 120f * scale, y - 255f * scale, 1, 3f * scale)
        }
        for (i in 0 until pCount) {
            if (plife[i] <= 0f) continue
            px[i] += pvx[i]
            py[i] += pvy[i]
            pvx[i] *= 0.96f
            pvy[i] = pvy[i] * 0.96f + 0.05f * scale
            plife[i] -= 0.016f
        }
    }

    fun draw(canvas: Canvas) {
        if (phase != Phase.HIDDEN) drawWizard(canvas)
        drawParticles(canvas)
    }

    // ---------------------------------------------------------------- desenho

    private fun easeOutBack(p: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val q = p - 1f
        return 1f + c3 * q * q * q + c1 * q * q
    }

    private fun drawWizard(canvas: Canvas) {
        var s = 1f
        var rot = 0f
        var a = 1f
        var dy = 0f
        var wave = sin(clock * 3f) * 0.22f
        when (phase) {
            Phase.APPEARING -> {
                val p = (t / APPEAR_TIME).coerceIn(0f, 1f)
                s = easeOutBack(p).coerceAtLeast(0f)
                a = (p * 3f).coerceIn(0f, 1f)
                rot = (1f - p) * -20f
                wave = sin(clock * 14f) * 0.45f * (1f - p) + sin(clock * 3f) * 0.22f * p
                drawMagicGlow(canvas, p)
            }
            Phase.LEAVING -> {
                val p = (t / LEAVE_TIME).coerceIn(0f, 1f)
                s = (1f - p).pow(1.4f)
                a = (1f - p * 0.6f)
                rot = p * p * 540f
                dy = -p * p * 160f * scale
                wave = sin(clock * 16f) * 0.5f
                drawMagicGlow(canvas, p)
            }
            else -> {}
        }
        if (s <= 0.01f) return

        val bob = if (phase == Phase.IDLE) sin(clock * 2.4f) * 6f * scale else 0f
        val layered = a < 0.99f
        if (layered) {
            canvas.saveLayerAlpha(
                x - 320f * scale, y - 520f * scale + dy, x + 320f * scale, y + 60f * scale + dy,
                (a * 255f).toInt()
            )
        }
        canvas.save()
        canvas.translate(x, y + dy + bob)
        canvas.scale(s * scale, s * scale * (1f + sin(clock * 2.4f) * 0.012f))
        canvas.rotate(rot, 0f, -170f)

        drawBody(canvas, wave)

        if (phase == Phase.IDLE) {
            for (i in 0 until 5) {
                val ang = clock * 0.9f + i * 1.2566f
                val sx = cos(ang) * (150f + 12f * sin(clock * 2f + i))
                val sy = -190f + sin(ang) * 70f
                val tw = 0.55f + 0.45f * sin(clock * 5f + i * 2f)
                fill.color = (((120 + 135 * tw).toInt() shl 24) or 0x00FFE27A)
                drawSparkle(canvas, sx, sy, 10f + 8f * tw)
            }
        }
        canvas.restore()
        if (layered) canvas.restore()
    }

    private fun drawMagicGlow(canvas: Canvas, p: Float) {
        val cx = x
        val cy = y - 170f * scale
        val pulse = sin(p * PI.toFloat())
        val base = 230f * scale
        fill.color = ((60f * pulse).toInt() shl 24) or 0x00B58CFF
        canvas.drawCircle(cx, cy, base * (0.55f + 0.6f * p), fill)
        fill.color = ((90f * pulse).toInt() shl 24) or 0x00FFB6F0
        canvas.drawCircle(cx, cy, base * (0.35f + 0.4f * p), fill)
        fill.color = ((140f * pulse).toInt() shl 24) or 0x00FFFFFF
        canvas.drawCircle(cx, cy, base * (0.15f + 0.2f * p), fill)
        stroke.color = ((200f * (1f - p)).toInt() shl 24) or 0x00FFFFFF
        stroke.strokeWidth = 8f * scale * (1f - p) + 1f
        canvas.drawCircle(cx, cy, base * (0.2f + 0.9f * p), stroke)
    }

    private fun drawParticles(canvas: Canvas) {
        for (i in 0 until pCount) {
            val l = plife[i]
            if (l <= 0f) continue
            val al = (l.coerceIn(0f, 1f) * 255f).toInt()
            fill.color = (al shl 24) or (pcol[i] and 0x00FFFFFF)
            if (i % 2 == 0) drawSparkleAt(canvas, px[i], py[i], psize[i] * (0.5f + l * 0.5f))
            else canvas.drawCircle(px[i], py[i], psize[i] * 0.35f * l, fill)
        }
    }

    private fun drawSparkleAt(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.save()
        canvas.translate(cx, cy)
        drawSparkle(canvas, 0f, 0f, r)
        canvas.restore()
    }

    private fun drawSparkle(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        path.reset()
        path.moveTo(cx, cy - r)
        path.lineTo(cx + r * 0.28f, cy - r * 0.28f)
        path.lineTo(cx + r, cy)
        path.lineTo(cx + r * 0.28f, cy + r * 0.28f)
        path.lineTo(cx, cy + r)
        path.lineTo(cx - r * 0.28f, cy + r * 0.28f)
        path.lineTo(cx - r, cy)
        path.lineTo(cx - r * 0.28f, cy - r * 0.28f)
        path.close()
        canvas.drawPath(path, fill)
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float, rot: Float) {
        path.reset()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.45f
            val ang = rot + i * (PI.toFloat() / 5f) - PI.toFloat() / 2f
            val px = cx + cos(ang) * rad
            val py = cy + sin(ang) * rad
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, fill)
    }

    private fun drawBody(canvas: Canvas, wave: Float) {
        val red = 0xFFE8453C.toInt()
        val darkRed = 0xFFB92E33.toInt()
        val gold = 0xFFF6C445.toInt()
        val skin = 0xFFFFD9BF.toInt()

        // sombra no chão
        fill.color = 0x33000000
        rect.set(-85f, -10f, 85f, 12f)
        canvas.drawOval(rect, fill)

        // pernas com meias listradas
        for (side in intArrayOf(-1, 1)) {
            val cx = side * 32f
            for (i in 0 until 5) {
                fill.color = if (i % 2 == 0) 0xFF5E8C61.toInt() else 0xFFE8C547.toInt()
                canvas.drawRect(cx - 11f, -52f + i * 8f, cx + 11f, -44f + i * 8f, fill)
            }
            fill.color = 0xFF3D5A40.toInt()
            rect.set(cx - 25f, -20f, cx + 25f, 6f)
            canvas.drawOval(rect, fill)
        }

        // gola traseira
        fill.color = darkRed
        rect.set(-84f, -184f, 84f, -132f)
        canvas.drawOval(rect, fill)

        // capa
        path.reset()
        path.moveTo(-58f, -150f)
        path.lineTo(58f, -150f)
        path.quadTo(100f, -90f, 114f, -34f)
        path.quadTo(0f, -10f, -114f, -34f)
        path.quadTo(-100f, -90f, -58f, -150f)
        path.close()
        fill.color = red
        canvas.drawPath(path, fill)
        path.reset()
        path.moveTo(26f, -150f)
        path.lineTo(58f, -150f)
        path.quadTo(100f, -90f, 114f, -34f)
        path.quadTo(76f, -23f, 36f, -18f)
        path.close()
        fill.color = 0x55902028
        canvas.drawPath(path, fill)

        // camisa listrada
        for (i in 0 until 6) {
            fill.color = if (i % 2 == 0) 0xFF2E8B8E.toInt() else 0xFFE98B3A.toInt()
            canvas.drawRect(-17f, -150f + i * 15f, 17f, -135f + i * 15f, fill)
        }
        fill.color = gold
        canvas.drawCircle(0f, -151f, 8f, fill)

        // estrelas na capa
        fill.color = gold
        drawStar(canvas, -72f, -100f, 13f, -0.2f)
        drawStar(canvas, 74f, -82f, 11f, 0.3f)
        drawStar(canvas, -44f, -50f, 10f, 0.1f)
        drawStar(canvas, 58f, -46f, 12f, -0.3f)
        drawStar(canvas, -92f, -58f, 7f, 0.2f)
        drawStar(canvas, 92f, -112f, 7f, 0f)

        // braço esquerdo (pendurado)
        fill.color = red
        rect.set(-100f, -148f, -64f, -80f)
        canvas.drawOval(rect, fill)
        fill.color = skin
        canvas.drawCircle(-84f, -76f, 11f, fill)

        // braço direito com varinha
        val ang = -1.05f + wave
        val hx = 62f + 52f * cos(ang)
        val hy = -136f + 52f * sin(ang)
        stroke.color = red
        stroke.strokeWidth = 30f
        canvas.drawLine(62f, -136f, hx, hy, stroke)
        fill.color = skin
        canvas.drawCircle(hx, hy, 12f, fill)
        val wx = hx + 62f * cos(ang)
        val wy = hy + 62f * sin(ang)
        stroke.color = 0xFF7A4B2A.toInt()
        stroke.strokeWidth = 7f
        canvas.drawLine(hx, hy, wx, wy, stroke)
        fill.color = 0x44FFE27A
        canvas.drawCircle(wx, wy, 26f, fill)
        fill.color = 0xFFFFE27A.toInt()
        drawSparkle(canvas, wx, wy, 17f + 4f * sin(clock * 9f))

        // cabeça
        fill.color = skin
        canvas.drawCircle(-71f, -208f, 11f, fill)
        canvas.drawCircle(71f, -208f, 11f, fill)
        rect.set(-72f, -275f, 72f, -143f)
        canvas.drawOval(rect, fill)

        // franja e cabelo
        fill.color = 0xFF4B2A27.toInt()
        path.reset()
        path.moveTo(-72f, -255f)
        path.lineTo(72f, -255f)
        path.lineTo(70f, -232f)
        path.quadTo(32f, -214f, 0f, -238f)
        path.quadTo(-32f, -214f, -70f, -232f)
        path.close()
        canvas.drawPath(path, fill)
        path.reset()
        path.moveTo(-72f, -250f); path.lineTo(-72f, -205f); path.quadTo(-62f, -215f, -58f, -238f); path.close()
        canvas.drawPath(path, fill)
        path.reset()
        path.moveTo(72f, -250f); path.lineTo(72f, -205f); path.quadTo(62f, -215f, 58f, -238f); path.close()
        canvas.drawPath(path, fill)

        // rosto
        fill.color = 0xFF1E1A22.toInt()
        rect.set(-33f, -216f, -19f, -198f); canvas.drawOval(rect, fill)
        rect.set(19f, -216f, 33f, -198f); canvas.drawOval(rect, fill)
        fill.color = Color.WHITE
        canvas.drawCircle(-24f, -211f, 3f, fill)
        canvas.drawCircle(28f, -211f, 3f, fill)
        fill.color = 0xAAFF8F8F.toInt()
        canvas.drawCircle(-46f, -190f, 18f, fill)
        canvas.drawCircle(46f, -190f, 18f, fill)
        fill.color = 0xFFF2A08E.toInt()
        canvas.drawCircle(0f, -194f, 4.5f, fill)
        stroke.color = 0xFF7A3B33.toInt()
        stroke.strokeWidth = 3f
        rect.set(-10f, -190f, 10f, -172f)
        canvas.drawArc(rect, 20f, 140f, false, stroke)

        // chapéu (balança de leve)
        canvas.save()
        canvas.rotate(sin(clock * 2f) * 2.5f, 0f, -240f)
        path.reset()
        path.moveTo(-80f, -240f)
        path.quadTo(-35f, -290f, 52f, -356f)
        path.quadTo(76f, -300f, 80f, -240f)
        path.quadTo(0f, -226f, -80f, -240f)
        path.close()
        fill.color = red
        canvas.drawPath(path, fill)
        path.reset()
        path.moveTo(52f, -356f)
        path.quadTo(76f, -300f, 80f, -240f)
        path.quadTo(48f, -234f, 22f, -236f)
        path.quadTo(44f, -300f, 52f, -356f)
        path.close()
        fill.color = 0x66902028
        canvas.drawPath(path, fill)
        fill.color = gold
        drawStar(canvas, -6f, -288f, 25f, -0.2f)
        drawStar(canvas, 50f, -262f, 9f, 0.2f)
        canvas.restore()
    }

    private companion object {
        const val APPEAR_TIME = 1.0f
        const val LEAVE_TIME = 0.85f
    }
}
