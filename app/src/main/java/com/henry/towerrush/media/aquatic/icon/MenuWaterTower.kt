package com.henry.towerrush.media.aquatic.icon

import android.graphics.Canvas
import com.henry.towerrush.media.aquatic.WaterBlockSkin

/**
 * Torre decorativa da TELA INICIAL ("Clique para jogar") com a skin aquática.
 * Reproduz o layout da sua tela: 7 blocos que vão estreitando para cima, em zigue-zague,
 * com a base cortada pela borda inferior da tela e variação de tom (claro / médio / escuro).
 */
class MenuWaterTower(val skin: WaterBlockSkin = WaterBlockSkin()) {

    private class Slot(val w: Float, val d: Float, val dx: Float, val shade: Float)

    // de CIMA para BAIXO (o último é o bloco-base 570 x 353,4)
    private val layout = arrayOf(
        Slot(150f, 150f, -0.078f, 0.00f),
        Slot(230f, 180f, +0.046f, 0.00f),
        Slot(300f, 214f, -0.035f, -0.45f),
        Slot(350f, 267f, +0.037f, 0.50f),
        Slot(420f, 295f, -0.029f, 0.00f),
        Slot(480f, 343f, +0.027f, 0.10f),
        Slot(570f, 353.4f, 0.000f, -0.45f)
    )

    fun draw(canvas: Canvas, screenW: Float, screenH: Float, timeMs: Long) {
        val scale = screenW * BASE_WIDTH_FRAC / skin.screenWidthUnits
        val step = skin.heightUnits * scale
        val cy0 = screenH * ANCHOR_Y
        val cxMid = screenW * 0.5f

        for (i in layout.indices.reversed()) {
            val s = layout[i]
            skin.draw(
                canvas,
                cxMid + s.dx * screenW,
                cy0 + i * step,
                scale,
                timeMs,
                seed = i + 1,
                w = s.w,
                d = s.d
            )
        }
    }

    private companion object {
        const val BASE_WIDTH_FRAC = 0.90f
        const val ANCHOR_Y = 0.43f
    }
}
