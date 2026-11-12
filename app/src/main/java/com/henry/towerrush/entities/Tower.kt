package com.henry.towerrush.entities

import android.graphics.Canvas

class Tower {
    val stackedBlocks = mutableListOf<Block>()

    fun addBlock(block: Block) {
        stackedBlocks.add(block)
    }

    fun getTopBlock(): Block? = stackedBlocks.lastOrNull()

    fun draw(canvas: Canvas) {
        drawWithMoving(canvas, null, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
    }

    /**
     * Desenha de baixo para cima, com a sombra de cada bloco caindo sobre o de baixo.
     * [viewTop]/[viewBottom] estão em coordenadas de mundo (já sem a câmera) e evitam
     * desenhar blocos fora da tela.
     */
    fun drawWithMoving(canvas: Canvas, moving: Block?, viewTop: Float, viewBottom: Float) {
        val list = stackedBlocks
        for (i in list.indices) {
            val b = list[i]
            if (b.maxY < viewTop || b.minY > viewBottom) continue
            if (i > 0) list[i - 1].drawShadowOf(canvas, b)
            b.draw(canvas)
        }
        if (moving != null) {
            list.lastOrNull()?.drawShadowOf(canvas, moving)
            moving.draw(canvas)
        }
    }

    fun clear() {
        stackedBlocks.clear()
    }
}
