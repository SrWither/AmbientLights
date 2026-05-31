package me.fadel.ambientlights.client.color


object ColorInterpolator {
    fun lerp(current: RGB, target: RGB, alpha: Float): RGB {
        return RGB(
            (current.r + (target.r - current.r) * alpha).toInt(),
            (current.g + (target.g - current.g) * alpha).toInt(),
            (current.b + (target.b - current.b) * alpha).toInt(),
            // Interpolamos también los blancos
            (current.w + (target.w - current.w) * alpha).toInt(),
            (current.c + (target.c - current.c) * alpha).toInt(),
            (current.dimming + (target.dimming - current.dimming) * alpha).toInt()
        )
    }
}