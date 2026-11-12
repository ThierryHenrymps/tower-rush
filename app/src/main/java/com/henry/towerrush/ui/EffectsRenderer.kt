package com.henry.towerrush.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.henry.towerrush.entities.Block
import com.henry.towerrush.entities.IsoProjection
import kotlin.random.Random

class EffectsRenderer {
    private class Debris(val block: Block, val vx: Float, var vy: Float, var life: Float)
    private class Ring(val v: FloatArray, val h: Float, var t: Float)
    private class Spark(
        var x: Float, var y: Float, val vx: Float, var vy: Float,
        var life: Float, val size: Float, val colorIndex: Int
    )

    private val debris = ArrayList<Debris>()
    private val rings = ArrayList<Ring>()
    private val sparks = ArrayList<Spark>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val sparkColors = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFD6FF.toInt(), 0xFFB8F2FF.toInt())

    fun clear() {
        debris.clear(); rings.clear(); sparks.clear()
    }

    fun spawnDebris(cutX: Float, cutWidth: Float, source: Block) {
        if (cutWidth < 2f) return
        val piece = Block(cutX, source.y, cutWidth, source.height, source.color, source.depth)
        val dir = if (cutX > source.x) 1f else -1f
        debris.add(Debris(piece, dir * source.height * 0.03f, -source.height * 0.04f, 1f))
    }

    fun spawnPerfect(b: Block) {
        val v = FloatArray(8)
        b.topVertices(v)
        rings.add(Ring(v, b.height, 0f))
        for (i in 0 until 14) {
            val u = Random.nextFloat()
            val w = Random.nextFloat()
            val x = v[0] + u * (v[2] - v[0]) + w * (v[6] - v[0])
            val y = v[1] + u * (v[3] - v[1]) + w * (v[7] - v[1])
            sparks.add(
                Spark(
                    x, y,
                    (Random.nextFloat() - 0.5f) * b.height * 0.06f,
                    -(0.02f + Random.nextFloat() * 0.05f) * b.height,
                    1f,
                    b.height * (0.03f + Random.nextFloat() * 0.03f),
                    i % sparkColors.size
                )
            )
        }
    }

    /** Chamado uma vez por frame (o jogo roda a ~60 fps). */
    fun update() {
        var i = debris.size - 1
        while (i >= 0) {
            val d = debris[i]
            d.vy += d.block.height * 0.012f
            d.block.x += d.vx / IsoProjection.ux
            d.block.y += d.vy
            d.life -= 0.022f
            d.block.alpha = (d.life.coerceIn(0f, 1f) * 255f).toInt()
            if (d.life <= 0f) debris.removeAt(i)
            i--
        }
        i = rings.size - 1
        while (i >= 0) {
            val r = rings[i]
            r.t += 0.045f
            if (r.t >= 1f) rings.removeAt(i)
            i--
        }
        i = sparks.size - 1
        while (i >= 0) {
            val s = sparks[i]
            s.x += s.vx
            s.y += s.vy
            s.vy += 0.015f * s.size
            s.life -= 0.025f
            if (s.life <= 0f) sparks.removeAt(i)
            i--
        }
    }

    fun draw(canvas: Canvas) {
        for (d in debris) d.block.draw(canvas)

        // plano translúcido que se expande a partir da face superior (como na referência)
        for (r in rings) {
            val v = r.v
            val cx = (v[0] + v[4]) * 0.5f
            val cy = (v[1] + v[5]) * 0.5f
            val sc = 1f + r.t * 0.55f
            path.reset()
            path.moveTo(cx + (v[0] - cx) * sc, cy + (v[1] - cy) * sc)
            path.lineTo(cx + (v[2] - cx) * sc, cy + (v[3] - cy) * sc)
            path.lineTo(cx + (v[4] - cx) * sc, cy + (v[5] - cy) * sc)
            path.lineTo(cx + (v[6] - cx) * sc, cy + (v[7] - cy) * sc)
            path.close()
            val fade = 1f - r.t
            paint.style = Paint.Style.FILL
            paint.color = ColorUtils.withAlpha(0xFFBFF5DC.toInt(), (fade * 120f).toInt())
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = r.h * 0.03f * fade + 1f
            paint.strokeJoin = Paint.Join.ROUND
            paint.color = ColorUtils.withAlpha(0xFFFFFFFF.toInt(), (fade * 220f).toInt())
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL

        for (s in sparks) {
            paint.color = ColorUtils.withAlpha(sparkColors[s.colorIndex], (s.life.coerceIn(0f, 1f) * 255f).toInt())
            canvas.drawCircle(s.x, s.y, s.size, paint)
        }
    }
}
