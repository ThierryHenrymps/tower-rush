package com.henry.towerrush.media.aquatic

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Skin aquática para um bloco isométrico com as proporções do seu modelo:
 *   Largura 570  x  Altura 167,2  x  Profundidade 353,4   (valores padrão abaixo).
 *
 * - Só Canvas: sem bitmaps, imagens ou bibliotecas. Tudo é desenhado por código.
 * - Leve: nenhum objeto é criado dentro de draw() (Paints, Paths, Shaders e arrays reaproveitados).
 * - Trabalha em "unidades do modelo" (570, 167,2, 353,4...) e só aplica uma escala em pixels,
 *   então o mesmo bloco serve para qualquer tamanho de tela.
 *
 * Orientação (padrão): a LARGURA (570) fica na face ESQUERDA e a PROFUNDIDADE (353,4) na
 * face DIREITA. Se no seu jogo for o contrário, troque os valores: draw(..., w = 353.4f, d = 570f).
 *
 * Uso:
 *   private val waterSkin = WaterBlockSkin()                     // crie UMA vez
 *   val scale = larguraDesejadaEmPx / waterSkin.screenWidthUnits  // px por unidade do modelo
 *   waterSkin.draw(canvas, cx, cy, scale, SystemClock.uptimeMillis(), block.id)
 *
 * (cx, cy) = centro do losango do TOPO do bloco, em pixels.
 * Para empilhar, suba cada bloco (altura * scale) pixels: cy - i * waterSkin.heightUnits * scale.
 */
class WaterBlockSkin(
    private val widthUnits: Float = 570f,
    val heightUnits: Float = 167.2f,
    private val depthUnits: Float = 353.4f,
    /** tan do ângulo das arestas: 0.5 = isométrico 2:1 (padrão); 0.577 = isométrico verdadeiro (30°). */
    private val slope: Float = 0.5f,
    var lowQuality: Boolean = false
) {
    private val cosT = (1.0 / sqrt(1.0 + slope * slope)).toFloat()
    private val sinT = slope * cosT

    /** Largura total do bloco na tela, em unidades do modelo (use para calcular a escala). */
    val screenWidthUnits: Float get() = (widthUnits + depthUnits) * cosT

    /** Altura total do bloco na tela (topo + lateral), em unidades do modelo. */
    val screenHeightUnits: Float get() = (widthUnits + depthUnits) * sinT + heightUnits

    // ---------- cores (pastel aquático) ----------
    private val cMidWater = 0xFF4DB6E6.toInt()
    private val cDeepWater = 0xFF2B7FBF.toInt()

    // gradientes em espaço unitário (0..1); a matriz os ajusta a cada face
    private val topShader = LinearGradient(
        0f, 0f, 1f, 1f,
        intArrayOf(0xFFD4F8FF.toInt(), 0xFF8EE3F5.toInt(), 0xFF5CC8EC.toInt()),
        floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
    )
    private val leftShader = LinearGradient(
        0f, 0f, 0f, 1f,
        intArrayOf(0xFFB4EEFA.toInt(), 0xFF63C9EC.toInt(), 0xFF3496D2.toInt()),
        floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
    )
    private val rightShader = LinearGradient(
        0f, 0f, 0f, 1f,
        intArrayOf(0xFF8EDDF2.toInt(), 0xFF45AEE0.toInt(), 0xFF2273B3.toInt()),
        floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
    )

    // ---------- objetos reaproveitados ----------
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val top = Path()
    private val left = Path()
    private val right = Path()
    private val wave = Path()
    private val ripple = Path()
    private val matrix = Matrix()
    private val pts = FloatArray(14 * 2)
    private val segs = FloatArray(13 * 4)
    private val twoPi = (2.0 * PI).toFloat()

    // vértices do topo (relativos ao centro do topo) e altura atual
    private var bX = 0f; private var bY = 0f   // vértice de trás
    private var rX = 0f; private var rY = 0f   // direita
    private var fX = 0f; private var fY = 0f   // frente
    private var lX = 0f; private var lY = 0f   // esquerda
    private var h = 0f

    /**
     * Desenha o bloco aquático animado.
     * @param scale pixels por unidade do modelo (ex.: 0.4f faz 570 -> 228 px).
     * @param seed use o id/índice do bloco para que cada um ondule em fase diferente.
     * @param w largura (face esquerda) e @param d profundidade (face direita); por padrão 570 e 353,4.
     */
    fun draw(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        scale: Float,
        timeMs: Long,
        seed: Int = 0,
        w: Float = widthUnits,
        d: Float = depthUnits,
        modelHeight: Float = heightUnits
    ) {
        if (scale <= 0f) return
        val t = (timeMs % 3_600_000L) / 1000f
        val phase = (seed * 1.7f) % twoPi
        h = modelHeight

        val ax = w * cosT; val ay = w * sinT      // aresta da face esquerda (desce para a direita)
        val bx = d * cosT; val by = d * sinT      // aresta da face direita (sobe para a direita)
        fX = (ax - bx) / 2f; fY = (ay + by) / 2f
        lX = fX - ax; lY = fY - ay
        rX = fX + bx; rY = fY - by
        bX = lX + bx; bY = lY - by

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)

        buildGeometry()

        // faces laterais
        matrix.setScale(1f, h + ay); matrix.postTranslate(0f, lY)
        leftShader.setLocalMatrix(matrix)
        matrix.setScale(1f, h + by); matrix.postTranslate(0f, rY)
        rightShader.setLocalMatrix(matrix)
        matrix.setScale(rX - lX, fY - bY); matrix.postTranslate(lX, bY)
        topShader.setLocalMatrix(matrix)

        drawSide(canvas, left, leftShader, true, lX, fX, lY, fY, (fX - lX), t, phase)
        drawSide(canvas, right, rightShader, false, fX, rX, fY, rY, (rX - fX), t, phase + 1.3f)
        drawTop(canvas, t, phase)

        // arestas de brilho (dão volume de "vidro com água")
        stroke.strokeWidth = h * 0.022f
        stroke.color = Color.argb(150, 255, 255, 255)
        canvas.drawPath(top, stroke)
        stroke.color = Color.argb(90, 255, 255, 255)
        canvas.drawLine(fX, fY, fX, fY + h, stroke)

        canvas.restore()
    }

    // ------------------------------------------------------------------ geometria
    private fun buildGeometry() {
        top.reset()
        top.moveTo(bX, bY); top.lineTo(rX, rY); top.lineTo(fX, fY); top.lineTo(lX, lY); top.close()
        left.reset()
        left.moveTo(lX, lY); left.lineTo(fX, fY); left.lineTo(fX, fY + h); left.lineTo(lX, lY + h); left.close()
        right.reset()
        right.moveTo(fX, fY); right.lineTo(rX, rY); right.lineTo(rX, rY + h); right.lineTo(fX, fY + h); right.close()
    }

    // ------------------------------------------------------------------ faces laterais
    private fun drawSide(
        c: Canvas, face: Path, shader: Shader, isLeft: Boolean,
        x0: Float, x1: Float, e0: Float, e1: Float, faceW: Float, t: Float, phase: Float
    ) {
        c.save()
        c.clipPath(face)

        paint.shader = shader
        paint.alpha = 255
        c.drawPath(face, paint)
        paint.shader = null

        val bottom = maxOf(e0, e1) + h + 40f

        // onda de fundo (mais lenta, só no modo normal): ~1 ciclo a cada 360 unidades
        if (!lowQuality) {
            waveLayer(c, x0, x1, e0, e1, bottom, h * 0.27f, h * 0.060f, faceW / 360f, 1.7f, t, phase,
                withAlpha(cMidWater, 120), 0, h * 0.02f)
        }
        // onda da frente com espuma: ~1 ciclo a cada 230 unidades
        waveLayer(c, x0, x1, e0, e1, bottom, h * 0.34f, h * 0.050f, faceW / 230f, -2.6f, t, phase + 2f,
            withAlpha(cDeepWater, 105), 210, h * 0.028f)

        if (!lowQuality) drawBubbles(c, x0, x1, e0, e1, faceW, t, phase)

        c.restore()
    }

    private fun waveLayer(
        c: Canvas, x0: Float, x1: Float, e0: Float, e1: Float, bottom: Float,
        inset: Float, amp: Float, waves: Float, speed: Float, t: Float, phase: Float,
        fill: Int, foamAlpha: Int, foamWidth: Float
    ) {
        val n = if (lowQuality) 6 else 12
        for (i in 0..n) {
            val u = i / n.toFloat()
            pts[i * 2] = x0 + (x1 - x0) * u
            pts[i * 2 + 1] = e0 + (e1 - e0) * u + inset + amp * sin(twoPi * waves * u + speed * t + phase)
        }
        wave.reset()
        wave.moveTo(pts[0], pts[1])
        for (i in 1..n) wave.lineTo(pts[i * 2], pts[i * 2 + 1])
        wave.lineTo(x1, bottom)
        wave.lineTo(x0, bottom)
        wave.close()
        paint.color = fill
        c.drawPath(wave, paint)

        if (foamAlpha > 0) {
            for (i in 0 until n) {
                segs[i * 4] = pts[i * 2]; segs[i * 4 + 1] = pts[i * 2 + 1]
                segs[i * 4 + 2] = pts[i * 2 + 2]; segs[i * 4 + 3] = pts[i * 2 + 3]
            }
            stroke.strokeWidth = foamWidth
            stroke.color = Color.argb(foamAlpha, 255, 255, 255)
            c.drawLines(segs, 0, n * 4, stroke)
        }
    }

    private fun drawBubbles(
        c: Canvas, x0: Float, x1: Float, e0: Float, e1: Float, faceW: Float, t: Float, phase: Float
    ) {
        val n = (faceW / 140f).roundToInt().coerceIn(2, 5)
        for (i in 0 until n) {
            val prog = (t * 0.30f + i * 0.37f + phase * 0.07f) % 1f
            val bx = x0 + (x1 - x0) * ((i + 0.5f) / n) + sin(t * 2.1f + i * 2.3f + phase) * 12f
            val u = ((bx - x0) / (x1 - x0)).coerceIn(0f, 1f)
            val bottomAt = e0 + (e1 - e0) * u + h
            val by = bottomAt - 0.08f * h - prog * h * 0.62f
            val r = (6f + 2.5f * (i % 3)) * (0.6f + 0.4f * prog)
            val a = (sin(prog * PI.toFloat()) * 190f).toInt().coerceIn(0, 255)
            stroke.strokeWidth = h * 0.012f
            stroke.color = Color.argb(a, 255, 255, 255)
            c.drawCircle(bx, by, r, stroke)
            paint.color = Color.argb(a / 4, 255, 255, 255)
            c.drawCircle(bx, by, r, paint)
        }
    }

    // ------------------------------------------------------------------ topo
    private fun drawTop(c: Canvas, t: Float, phase: Float) {
        c.save()
        c.clipPath(top)

        paint.shader = topShader
        paint.alpha = 255
        c.drawPath(top, paint)
        paint.shader = null

        if (!lowQuality) {
            // ondulações em paralelogramo que se expandem do centro
            stroke.strokeWidth = h * 0.02f
            for (i in 0 until 3) {
                val s = (t * 0.38f + i / 3f + phase * 0.1f) % 1f
                stroke.color = Color.argb(((1f - s) * 150f).toInt(), 255, 255, 255)
                ripple.reset()
                ripple.moveTo(bX * s, bY * s); ripple.lineTo(rX * s, rY * s)
                ripple.lineTo(fX * s, fY * s); ripple.lineTo(lX * s, lY * s); ripple.close()
                c.drawPath(ripple, stroke)
            }
            // brilhos cintilantes (posições em coordenadas p,q do topo)
            for (i in 0 until 3) {
                val v = sin(t * 2.7f + i * 2.1f + phase)
                if (v > 0.6f) {
                    val p = SPARK_P[i]; val q = SPARK_Q[i]
                    val sx = bX + p * (rX - bX) + q * (lX - bX)
                    val sy = bY + p * (rY - bY) + q * (lY - bY)
                    paint.color = Color.argb(((v - 0.6f) / 0.4f * 230f).toInt(), 255, 255, 255)
                    c.drawCircle(sx, sy, h * 0.04f, paint)
                }
            }
        }
        c.restore()
    }

    private fun withAlpha(color: Int, a: Int) =
        Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))

    private companion object {
        val SPARK_P = floatArrayOf(0.30f, 0.70f, 0.55f)
        val SPARK_Q = floatArrayOf(0.40f, 0.30f, 0.75f)
    }
}
