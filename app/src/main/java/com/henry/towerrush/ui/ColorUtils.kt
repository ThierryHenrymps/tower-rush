package com.henry.towerrush.ui

import android.graphics.Color

object ColorUtils {
    fun lighten(value: Int, amount: Float): Int = Color.argb(
        Color.alpha(value),
        (Color.red(value) + (255 - Color.red(value)) * amount).toInt(),
        (Color.green(value) + (255 - Color.green(value)) * amount).toInt(),
        (Color.blue(value) + (255 - Color.blue(value)) * amount).toInt()
    )

    fun darken(value: Int, amount: Float): Int = Color.argb(
        Color.alpha(value),
        (Color.red(value) * (1f - amount)).toInt(),
        (Color.green(value) * (1f - amount)).toInt(),
        (Color.blue(value) * (1f - amount)).toInt()
    )

    fun withAlpha(value: Int, alpha: Int): Int =
        (value and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
}
