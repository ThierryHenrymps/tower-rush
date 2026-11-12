package com.henry.towerrush.entities

import kotlin.math.min

/**
 * Projeção isométrica 2:1 usada só para desenhar. A lógica continua em x/y/width:
 * o eixo "x" do motor vira o eixo longo do bloco (sobe para a direita na tela) e a
 * profundidade do bloco é constante, como no jogo de referência.
 */
object IsoProjection {
    private const val SCALE = 0.8f

    var ux: Float = 0.894f * SCALE   // deslocamento horizontal por unidade de largura
    var uy: Float = 0.447f * SCALE   // deslocamento vertical (para cima) por unidade de largura
    var ox: Float = 0f               // origem horizontal na tela
    var xRef: Float = 0f             // x do bloco base (referência vertical)
    var depth: Float = 300f          // profundidade padrão dos blocos (unidades do motor)
    var version: Int = 0
        private set

    fun configure(screenWidth: Float, blockHeight: Float, defaultWidth: Float) {
        depth = defaultWidth * 0.62f
        val baseX = (screenWidth - defaultWidth) / 2f - blockHeight * 0.46f
        xRef = baseX
        val centered = screenWidth / 2f - (defaultWidth + depth) * ux / 2f - baseX * ux
        val limit = screenWidth * 0.97f - (screenWidth - blockHeight * 0.5f + depth) * ux
        ox = min(centered, limit)
        version++
    }
}
