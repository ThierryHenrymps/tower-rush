package com.henry.towerrush.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/**
 * Cenário em camadas com paralaxe: céu, estrelas, ilhas flutuantes e nuvens em várias
 * profundidades, partículas e nuvens de primeiro plano. Ilhas e nuvens são sprites
 * gerados uma vez por tamanho de tela; por frame só há drawBitmap/drawCircle.
 */
class BackgroundRenderer {

    private class Item(
        val cloud: Boolean, val sprite: Int,
        val xf: Float, val yf: Float, val size: Float,
        val depth: Float, val speed: Float, val phase: Float, val alpha: Int
    )

    private var w = 0f
    private var h = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val beamPath = Path()

    private var skyShader: Shader? = null
    private var glowShader: Shader? = null
    private var beamShader: Shader? = null
    private var gameSkyShader: Shader? = null
    private var gameBeamShader: Shader? = null
    private val gameBeam1 = Path()
    private val gameBeam2 = Path()
    private val bubX = FloatArray(22)
    private val bubY = FloatArray(22)
    private val bubR = FloatArray(22)
    private val bubSpeed = FloatArray(22)
    private val bubPhase = FloatArray(22)
    private var hazeFar: ColorFilter? = null
    private var hazeMid: ColorFilter? = null

    private var islandSprites: Array<Bitmap> = emptyArray()
    private var cloudSprites: Array<Bitmap> = emptyArray()
    private val items = ArrayList<Item>()
    private val frontItems = ArrayList<Item>()

    private val starX = FloatArray(44)
    private val starY = FloatArray(44)
    private val starS = FloatArray(44)
    private val partX = FloatArray(34)
    private val partY = FloatArray(34)
    private val partSpeed = FloatArray(34)
    private val partSize = FloatArray(34)
    private val partPhase = FloatArray(34)
    private val partColors = intArrayOf(0xFFFFFFFF.toInt(), 0xFFE2C9FF.toInt(), 0xFFA8EEFF.toInt())

    init {
        val r = Random(7)
        for (i in starX.indices) {
            starX[i] = r.nextFloat(); starY[i] = r.nextFloat() * 0.65f
            starS[i] = 0.7f + r.nextFloat() * 1.3f
        }
        for (i in bubX.indices) {
            bubX[i] = r.nextFloat(); bubY[i] = r.nextFloat()
            bubR[i] = 0.004f + r.nextFloat() * 0.009f
            bubSpeed[i] = 0.4f + r.nextFloat() * 0.9f
            bubPhase[i] = r.nextFloat() * 6.28f
        }
        for (i in partX.indices) {
            partX[i] = r.nextFloat(); partY[i] = r.nextFloat()
            partSpeed[i] = 0.3f + r.nextFloat() * 0.9f
            partSize[i] = 1.2f + r.nextFloat() * 2.4f
            partPhase[i] = r.nextFloat() * 6.28f
        }
    }

    /**
     * [gameBlend] 0 = mundo roxo do menu; 1 = degradê verde-água da partida (com feixes de luz e bolhas).
     */
    fun draw(
        canvas: Canvas, width: Float, height: Float, time: Long,
        cameraOffset: Float = 0f, gameBlend: Float = 0f
    ) {
        ensure(width, height)
        if (gameBlend < 0.999f) drawPurpleWorld(canvas, time, cameraOffset)
        if (gameBlend > 0.001f) drawGameScene(canvas, time, cameraOffset, gameBlend)
    }

    private fun drawPurpleWorld(canvas: Canvas, time: Long, cameraOffset: Float) {
        paint.shader = skyShader
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = glowShader
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = beamShader
        canvas.drawPath(beamPath, paint)
        paint.shader = null

        // Quanto mais alto a torre sobe, mais escuro e estrelado fica o céu.
        val tint = (cameraOffset / (h * 10f)).coerceIn(0f, 1f)
        if (tint > 0.01f) {
            paint.color = Color.argb((tint * 110f).toInt(), 6, 2, 22)
            canvas.drawRect(0f, 0f, w, h, paint)
        }

        drawStars(canvas, time, 0.6f + 0.4f * tint)
        for (item in items) drawItem(canvas, item, time, cameraOffset, 1f)
        drawParticles(canvas, time, cameraOffset)
    }

    private fun drawGameScene(canvas: Canvas, time: Long, off: Float, blend: Float) {
        val a255 = (blend * 255f).toInt().coerceIn(0, 255)
        paint.shader = gameSkyShader
        paint.alpha = a255
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // ao subir, o céu fica um pouco mais profundo
        val tint = (off / (h * 12f)).coerceIn(0f, 1f)
        if (tint > 0.01f) {
            paint.color = Color.argb((tint * 90f * blend).toInt(), 12, 26, 60)
            canvas.drawRect(0f, 0f, w, h, paint)
        }

        // feixes de luz
        val sway = sin(time / 4200.0).toFloat() * w * 0.02f
        canvas.save()
        canvas.translate(sway, 0f)
        paint.shader = gameBeamShader
        paint.alpha = a255
        canvas.drawPath(gameBeam1, paint)
        canvas.drawPath(gameBeam2, paint)
        paint.shader = null
        canvas.restore()

        // bolhas
        val u = w / 1080f
        for (i in bubX.indices) {
            val y = mod(frac(bubY[i] - time * bubSpeed[i] * 0.000018f) * h + off * 0.5f, h)
            val x = bubX[i] * w + sin(time / 1700.0 + bubPhase[i]).toFloat() * w * 0.01f
            paint.color = Color.argb((120f * blend).toInt(), 220, 242, 255)
            canvas.drawCircle(x, y, bubR[i] * 1080f * u, paint)
        }
        paint.alpha = 255
    }

    /** Nuvens à frente da torre (chamar depois de desenhar a torre). */
    fun drawForeground(canvas: Canvas, time: Long, cameraOffset: Float, gameBlend: Float = 0f) {
        if (gameBlend > 0.99f) return
        val scale = 1f - gameBlend
        for (item in frontItems) drawItem(canvas, item, time, cameraOffset, scale)
    }

    private fun mod(a: Float, b: Float): Float {
        val r = a % b
        return if (r < 0f) r + b else r
    }

    private fun frac(v: Float): Float = v - floor(v)

    private fun drawStars(canvas: Canvas, time: Long, boost: Float) {
        paint.color = Color.WHITE
        for (i in starX.indices) {
            val pulse = 0.55f + 0.45f * sin(time / 600.0 + i).toFloat()
            paint.alpha = ((80f + 175f * pulse) * boost).toInt().coerceIn(0, 255)
            canvas.drawCircle(starX[i] * w, starY[i] * h, starS[i], paint)
        }
        paint.alpha = 255
    }

    private fun drawParticles(canvas: Canvas, time: Long, off: Float) {
        for (i in partX.indices) {
            val y = mod(frac(partY[i] - time * partSpeed[i] * 0.00003f) * h + off * 0.6f, h)
            val x = partX[i] * w + sin(time / 1300.0 + partPhase[i]).toFloat() * w * 0.012f
            val tw = 0.6f + 0.4f * sin(time / 700.0 + partPhase[i]).toFloat()
            val c = partColors[i % partColors.size]
            paint.color = ColorUtils.withAlpha(c, (30f * tw).toInt())
            canvas.drawCircle(x, y, partSize[i] * 3f, paint)
            paint.color = ColorUtils.withAlpha(c, (200f * tw).toInt())
            canvas.drawCircle(x, y, partSize[i], paint)
        }
    }

    private fun drawItem(canvas: Canvas, it: Item, time: Long, off: Float, alphaScale: Float) {
        val sprites = if (it.cloud) cloudSprites else islandSprites
        if (sprites.isEmpty()) return
        val bmp = sprites[it.sprite % sprites.size]
        val scale = it.size * w / bmp.width
        val sw = bmp.width * scale
        val sh = bmp.height * scale

        val bob = if (it.cloud) 0f else sin(time / 1500.0 + it.phase).toFloat() * h * 0.006f
        val y0 = it.yf * h + off * it.depth + bob
        val span = h * 1.4f
        val y = mod(y0 + sh, span) - sh
        if (y > h || y + sh < 0f) return

        val x = if (it.cloud) {
            mod(it.xf * w + time * it.speed * w / 1000f, w + sw) - sw
        } else {
            it.xf * w - sw * 0.5f
        }
        if (x > w || x + sw < 0f) return

        bitmapPaint.alpha = (it.alpha * alphaScale).toInt().coerceIn(0, 255)
        bitmapPaint.colorFilter = when {
            it.cloud -> null
            it.depth < 0.2f -> hazeFar
            it.depth < 0.4f -> hazeMid
            else -> null
        }
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bmp, 0f, 0f, bitmapPaint)
        canvas.restore()
        bitmapPaint.colorFilter = null
        bitmapPaint.alpha = 255
    }

    // ---------------------------------------------------------------- construção

    private fun ensure(width: Float, height: Float) {
        if (width == w && height == h && islandSprites.isNotEmpty()) return
        w = width
        h = height

        skyShader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF080320.toInt(), 0xFF1B0A58.toInt(), 0xFF3F1C9A.toInt(), 0xFF6A3FD0.toInt()),
            floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        glowShader = RadialGradient(
            w * 0.5f, h * 0.62f, w * 0.95f,
            intArrayOf(0x667B4DFF, 0x227B4DFF, 0x007B4DFF),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        beamShader = LinearGradient(
            0f, 0f, w * 0.75f, h,
            intArrayOf(0x24D9B5FF, 0x087F55FF, 0x007F55FF), null, Shader.TileMode.CLAMP
        )
        beamPath.reset()
        beamPath.moveTo(w * 0.05f, 0f); beamPath.lineTo(w * 0.30f, 0f)
        beamPath.lineTo(w * 0.88f, h); beamPath.lineTo(w * 0.62f, h); beamPath.close()

        gameSkyShader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF35506B.toInt(), 0xFF3E6F79.toInt(), 0xFF55A58F.toInt(), 0xFF74E3A4.toInt(), 0xFF8CFFC0.toInt()),
            floatArrayOf(0f, 0.30f, 0.55f, 0.78f, 1f), Shader.TileMode.CLAMP
        )
        gameBeamShader = LinearGradient(
            0f, 0f, 0f, h * 0.6f, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
        )
        gameBeam1.reset()
        gameBeam1.moveTo(w * 0.09f, 0f); gameBeam1.lineTo(w * 0.24f, 0f)
        gameBeam1.lineTo(-w * 0.12f, h * 0.6f); gameBeam1.lineTo(-w * 0.30f, h * 0.6f); gameBeam1.close()
        gameBeam2.reset()
        gameBeam2.moveTo(w * 0.14f, 0f); gameBeam2.lineTo(w * 0.24f, 0f)
        gameBeam2.lineTo(w * 0.42f, h * 0.55f); gameBeam2.lineTo(w * 0.30f, h * 0.55f); gameBeam2.close()

        hazeFar = PorterDuffColorFilter(0x889A6BFF.toInt(), PorterDuff.Mode.SRC_ATOP)
        hazeMid = PorterDuffColorFilter(0x449A6BFF, PorterDuff.Mode.SRC_ATOP)

        for (b in islandSprites) b.recycle()
        for (b in cloudSprites) b.recycle()
        val iw = (w * 0.42f).toInt().coerceAtLeast(64)
        val ih = (w * 0.46f).toInt().coerceAtLeast(64)
        val cw = (w * 0.55f).toInt().coerceAtLeast(64)
        val ch = (w * 0.26f).toInt().coerceAtLeast(32)
        islandSprites = Array(3) { buildIslandSprite(iw, ih, it) }
        cloudSprites = Array(3) { buildCloudSprite(cw, ch, it) }

        items.clear()
        // sprite, xf, yf(topo), tamanho (fração da largura), profundidade, vel., fase, alpha
        items.add(Item(false, 2, 0.27f, 0.42f, 0.10f, 0.10f, 0f, 0.5f, 200))
        items.add(Item(false, 0, 0.90f, 0.28f, 0.20f, 0.14f, 0f, 1.7f, 215))
        items.add(Item(true, 0, 0.10f, 0.24f, 0.55f, 0.08f, 0.004f, 0f, 120))
        items.add(Item(false, 0, 0.05f, 0.36f, 0.36f, 0.30f, 0f, 2.4f, 240))
        items.add(Item(true, 1, 0.88f, 0.40f, 0.45f, 0.25f, 0.008f, 0f, 150))
        items.add(Item(false, 2, 0.94f, 0.50f, 0.30f, 0.35f, 0f, 3.1f, 245))
        items.add(Item(true, 2, 0.12f, 0.56f, 0.50f, 0.35f, 0.006f, 0f, 170))
        items.add(Item(false, 1, 0.08f, 0.66f, 0.22f, 0.45f, 0f, 4.2f, 250))
        items.add(Item(false, 1, 0.94f, 0.74f, 0.18f, 0.50f, 0f, 5.0f, 250))
        items.add(Item(true, 0, 0.90f, 0.64f, 0.50f, 0.45f, 0.010f, 0f, 170))
        items.add(Item(true, 1, 0.08f, 0.80f, 0.55f, 0.55f, 0.007f, 0f, 190))
        items.sortBy { it.depth }

        frontItems.clear()
        frontItems.add(Item(true, 2, 0.85f, 0.93f, 0.70f, 1.05f, 0.012f, 0f, 225))
        frontItems.add(Item(true, 0, 0.05f, 0.98f, 0.80f, 1.15f, 0.009f, 0f, 235))
        frontItems.add(Item(true, 1, 0.55f, 1.02f, 0.90f, 1.25f, 0.010f, 0f, 215))
    }

    private fun buildIslandSprite(sw: Int, sh: Int, variant: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val w = sw.toFloat()
        val h = sh.toFloat()
        val gy = h * 0.16f
        val tip = when (variant) { 0 -> 0.50f; 1 -> 0.42f; else -> 0.58f }

        val rock = Path().apply {
            moveTo(w * 0.03f, gy + h * 0.05f)
            lineTo(w * 0.97f, gy + h * 0.05f)
            lineTo(w * 0.88f, gy + h * 0.26f)
            lineTo(w * 0.74f, gy + h * 0.44f)
            lineTo(w * (tip + 0.10f), gy + h * 0.64f)
            lineTo(w * tip, h * 0.99f)
            lineTo(w * (tip - 0.12f), gy + h * 0.62f)
            lineTo(w * 0.26f, gy + h * 0.42f)
            lineTo(w * 0.12f, gy + h * 0.24f)
            close()
        }
        p.shader = LinearGradient(
            0f, gy, w, gy + h * 0.7f,
            intArrayOf(0xFFC27AA6.toInt(), 0xFF8E5A9E.toInt(), 0xFF4A3787.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        c.drawPath(rock, p)
        p.shader = null

        val darkFacet = Path().apply {
            moveTo(w * 0.62f, gy + h * 0.05f); lineTo(w * 0.97f, gy + h * 0.05f)
            lineTo(w * 0.88f, gy + h * 0.26f); lineTo(w * 0.74f, gy + h * 0.44f)
            lineTo(w * (tip + 0.10f), gy + h * 0.64f); lineTo(w * 0.60f, gy + h * 0.30f)
            close()
        }
        p.color = 0x40200850
        c.drawPath(darkFacet, p)
        val lightFacet = Path().apply {
            moveTo(w * 0.03f, gy + h * 0.05f); lineTo(w * 0.30f, gy + h * 0.05f)
            lineTo(w * 0.26f, gy + h * 0.42f); lineTo(w * 0.12f, gy + h * 0.24f)
            close()
        }
        p.color = 0x22FFD9F0
        c.drawPath(lightFacet, p)

        // grama: borda mais escura embaixo e topo claro
        p.color = 0xFF3B7A3E.toInt()
        c.drawOval(RectF(w * 0.02f, gy + h * 0.01f, w * 0.98f, gy + h * 0.13f), p)
        p.shader = LinearGradient(
            0f, gy - h * 0.05f, 0f, gy + h * 0.10f,
            0xFF9BE070.toInt(), 0xFF4F9E48.toInt(), Shader.TileMode.CLAMP
        )
        c.drawOval(RectF(0f, gy - h * 0.05f, w, gy + h * 0.10f), p)
        p.shader = null

        val xs: FloatArray
        val sizes: FloatArray
        when (variant) {
            0 -> { xs = floatArrayOf(0.22f, 0.42f, 0.70f); sizes = floatArrayOf(1.0f, 0.8f, 1.15f) }
            1 -> { xs = floatArrayOf(0.30f, 0.62f); sizes = floatArrayOf(1.1f, 0.9f) }
            else -> { xs = floatArrayOf(0.18f, 0.50f, 0.78f); sizes = floatArrayOf(0.8f, 1.2f, 0.9f) }
        }
        val baseY = gy + h * 0.02f
        for (i in xs.indices) {
            val x = w * xs[i]
            val s = w * 0.085f * sizes[i]
            p.color = 0xFF5E4033.toInt()
            c.drawRect(x - s * 0.09f, baseY - s * 0.55f, x + s * 0.09f, baseY, p)
            p.color = 0xFF3F8F4A.toInt()
            c.drawCircle(x, baseY - s * 0.85f, s * 0.5f, p)
            p.color = 0xFF63B85A.toInt()
            c.drawCircle(x - s * 0.12f, baseY - s * 1.0f, s * 0.38f, p)
        }
        return bmp
    }

    private fun buildCloudSprite(sw: Int, sh: Int, variant: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(100 + variant)
        val w = sw.toFloat()
        val h = sh.toFloat()

        // base mais escura (volume) e topo claro
        for (i in 0 until 7) {
            val cx = w * (0.12f + 0.76f * i / 6f)
            val r = h * (0.28f + 0.14f * rnd.nextFloat())
            val cy = h * 0.62f
            p.shader = RadialGradient(
                cx, cy, r, intArrayOf(0xCC9468E8.toInt(), 0x009468E8), null, Shader.TileMode.CLAMP
            )
            c.drawCircle(cx, cy, r, p)
        }
        for (i in 0 until 6) {
            val cx = w * (0.18f + 0.64f * i / 5f) + (rnd.nextFloat() - 0.5f) * w * 0.05f
            val r = h * (0.30f + 0.20f * rnd.nextFloat())
            val cy = h * (0.42f - 0.10f * sin(i * 1.3f))
            p.shader = RadialGradient(
                cx, cy, r, intArrayOf(0xDDEBDCFF.toInt(), 0x00EBDCFF), null, Shader.TileMode.CLAMP
            )
            c.drawCircle(cx, cy, r, p)
        }
        p.shader = null
        return bmp
    }
}
