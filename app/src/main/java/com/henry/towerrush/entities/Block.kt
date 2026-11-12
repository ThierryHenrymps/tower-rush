package com.henry.towerrush.entities

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.SystemClock
import com.henry.towerrush.ui.ColorUtils
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max

/**
 * Prisma isométrico com três faces visíveis (topo, esquerda, direita), no estilo da referência.
 * A colisão usa apenas x/width/height. Paths e shaders ficam em cache (coordenadas locais)
 * e só são refeitos se a largura ou a projeção mudarem.
 */
class Block(
    var x: Float,
    var y: Float,
    var width: Float,
    val height: Float,
    val color: Int,
    val depth: Float = IsoProjection.depth
) {
    /** 0..255 – usado nos pedaços cortados que caem. */
    var alpha: Int = 255

    /** Marca (uptimeMillis) do encaixe; 0 = sem animação. */
    var landedAtMs: Long = 0L

    // posição do vértice esquerdo da face superior, em coordenadas de mundo/tela
    val ax: Float get() = IsoProjection.ox + x * IsoProjection.ux
    val ay: Float get() = y - (x - IsoProjection.xRef) * IsoProjection.uy
    val minY: Float get() = ay - width * IsoProjection.uy
    val maxY: Float get() = ay + depth * IsoProjection.uy + height
    val maxX: Float get() = ax + (width + depth) * IsoProjection.ux

    private var builtWidth = -1f
    private var builtVersion = -1
    private var lx = 0f
    private var ly = 0f
    private var dxp = 0f
    private var dyp = 0f

    private val topPath = Path()
    private val leftPath = Path()
    private val rightPath = Path()
    private val shadowPath = Path()

    private val topPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x80FFFFFF.toInt()
        strokeWidth = max(1.5f, height * 0.018f)
        strokeCap = Paint.Cap.ROUND
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x38FFFFFF
        strokeWidth = max(1.2f, height * 0.012f)
        strokeCap = Paint.Cap.ROUND
    }

    /** Escreve os 4 vértices da face superior (A, B, C, D) em [out] (8 floats, coordenadas de mundo). */
    fun topVertices(out: FloatArray) {
        val a = ax
        val b = ay
        val u = IsoProjection.ux
        val v = IsoProjection.uy
        val l1 = width * u
        val l2 = width * v
        val d1 = depth * u
        val d2 = depth * v
        out[0] = a; out[1] = b
        out[2] = a + l1; out[3] = b - l2
        out[4] = a + l1 + d1; out[5] = b - l2 + d2
        out[6] = a + d1; out[7] = b + d2
    }

    private fun rebuild() {
        builtWidth = width
        builtVersion = IsoProjection.version
        val h = height
        lx = width * IsoProjection.ux
        ly = width * IsoProjection.uy
        dxp = depth * IsoProjection.ux
        dyp = depth * IsoProjection.uy

        topPath.reset()
        topPath.moveTo(0f, 0f); topPath.lineTo(lx, -ly)
        topPath.lineTo(lx + dxp, -ly + dyp); topPath.lineTo(dxp, dyp); topPath.close()

        leftPath.reset()
        leftPath.moveTo(0f, 0f); leftPath.lineTo(dxp, dyp)
        leftPath.lineTo(dxp, dyp + h); leftPath.lineTo(0f, h); leftPath.close()

        rightPath.reset()
        rightPath.moveTo(dxp, dyp); rightPath.lineTo(lx + dxp, -ly + dyp)
        rightPath.lineTo(lx + dxp, -ly + dyp + h); rightPath.lineTo(dxp, dyp + h); rightPath.close()

        topPaint.shader = LinearGradient(
            0f, 0f, lx + dxp, -ly + dyp,
            ColorUtils.lighten(color, 0.30f), ColorUtils.lighten(color, 0.16f), Shader.TileMode.CLAMP
        )
        leftPaint.shader = LinearGradient(
            0f, 0f, 0f, dyp + h,
            ColorUtils.lighten(color, 0.04f), ColorUtils.darken(color, 0.06f), Shader.TileMode.CLAMP
        )
        rightPaint.shader = LinearGradient(
            0f, -ly + dyp, 0f, dyp + h,
            ColorUtils.darken(color, 0.14f), ColorUtils.darken(color, 0.26f), Shader.TileMode.CLAMP
        )
    }

    private fun squash(): Float {
        if (landedAtMs == 0L) return 0f
        val t = (SystemClock.uptimeMillis() - landedAtMs) / 1000f
        if (t > 0.6f) { landedAtMs = 0L; return 0f }
        return 0.10f * exp(-6f * t) * cos(16f * t)
    }

    fun draw(canvas: Canvas) {
        if (builtWidth != width || builtVersion != IsoProjection.version) rebuild()
        val layered = alpha < 255
        val px = ax
        val py = ay
        if (layered) {
            canvas.saveLayerAlpha(px - 4f, minY - 4f, maxX + 4f, maxY + 4f, alpha)
        }
        canvas.save()
        canvas.translate(px, py)
        val sq = squash()
        if (sq != 0f) canvas.scale(1f + sq * 0.3f, 1f - sq, (lx + dxp) * 0.5f, dyp + height)

        canvas.drawPath(leftPath, leftPaint)
        canvas.drawPath(rightPath, rightPaint)
        canvas.drawPath(topPath, topPaint)
        // arestas iluminadas (luz vem de cima à esquerda)
        canvas.drawLine(0f, 0f, lx, -ly, rimPaint)
        canvas.drawLine(0f, 0f, dxp, dyp, rimPaint)
        canvas.drawLine(dxp, dyp, dxp, dyp + height, cornerPaint)
        canvas.restore()
        if (layered) canvas.restore()
    }

    /** Sombra projetada por [upper] sobre a face superior deste bloco. */
    fun drawShadowOf(canvas: Canvas, upper: Block) {
        if (builtWidth != width || builtVersion != IsoProjection.version) rebuild()
        canvas.save()
        canvas.translate(ax, ay)
        canvas.clipPath(topPath)
        val ox = upper.ax - ax
        val oy = upper.ay + upper.height - ay
        val sx = upper.height * 0.45f * 0.894f
        val sy = upper.height * 0.45f * 0.447f
        shadowPaint.color = 0x22150038
        drawFootprint(canvas, ox + sx * 1.8f, oy + sy * 1.8f, upper)
        shadowPaint.color = 0x44150038
        drawFootprint(canvas, ox + sx, oy + sy, upper)
        canvas.restore()
    }

    private fun drawFootprint(canvas: Canvas, px: Float, py: Float, u: Block) {
        val l1 = u.width * IsoProjection.ux
        val l2 = u.width * IsoProjection.uy
        val d1 = u.depth * IsoProjection.ux
        val d2 = u.depth * IsoProjection.uy
        shadowPath.reset()
        shadowPath.moveTo(px, py); shadowPath.lineTo(px + l1, py - l2)
        shadowPath.lineTo(px + l1 + d1, py - l2 + d2); shadowPath.lineTo(px + d1, py + d2)
        shadowPath.close()
        canvas.drawPath(shadowPath, shadowPaint)
    }
}
