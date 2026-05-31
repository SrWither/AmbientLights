package me.fadel.ambientlights.client.lighting

import me.fadel.ambientlights.client.color.RGB
import me.fadel.ambientlights.client.color.ColorInterpolator
import net.minecraft.client.Minecraft
import net.minecraft.world.level.Level

object EnviromentColorProvider {

    private const val SECONDARY_TIME_OFFSET = 600L

    // Amanecer: naranja cálido, no muy brillante todavía
    private val dawn         = RGB(210, 80,  0,   w = 200, c = 0,   dimming = 65)
    // Día: blanco brillante con toque celeste — c alto domina, azul añade el matiz
    private val day          = RGB(20,  60,  130, w = 60,  c = 230, dimming = 100)
    // Atardecer: ámbar/naranja intenso cuando el sol baja
    private val sunsetOrange = RGB(230, 90,  0,   w = 150, c = 0,   dimming = 95)
    // Ocaso oscuro: misma familia cálida pero muy dim — baja el brillo ANTES de virar al azul
    private val duskDark     = RGB(80,  15,  5,   w = 5,   c = 0,   dimming = 22)
    // Noche: azul oscuro profundo
    private val night        = RGB(5,   10,  80,  w = 0,   c = 0,   dimming = 20)

    private fun overworldColor(dayTime: Long): RGB {
        val time = (dayTime % 24000).toInt()

        return when {
            time < 6000  -> ColorInterpolator.lerp(dawn,         day,          time / 6000f)
            time < 11000 -> day
            time < 13000 -> ColorInterpolator.lerp(day,          sunsetOrange, (time - 11000) / 2000f)
            time < 13600 -> ColorInterpolator.lerp(sunsetOrange, duskDark,    (time - 13000) / 600f)
            time < 14000 -> ColorInterpolator.lerp(duskDark,     night,       (time - 13600) / 400f)
            time < 22000 -> night
            else         -> ColorInterpolator.lerp(night,        dawn,         (time - 22000) / 2000f)
        }
    }

    fun primaryColor(client: Minecraft): RGB {
        val level = client.level ?: return RGB(0, 0, 0, w = 150, c = 150, dimming = 100)

        return when (level.dimension()) {
            Level.NETHER -> RGB(220, 20,  0,   w = 20, c = 0, dimming = 80)
            Level.END    -> RGB(90,  10,  180, w = 0,  c = 0, dimming = 60)
            else         -> overworldColor(level.overworldClockTime)
        }
    }

    fun secondaryColor(client: Minecraft): RGB {
        val level = client.level ?: return RGB(0, 0, 0, w = 200, c = 80, dimming = 95)

        return when (level.dimension()) {
            // Más naranja/amarillo que el primario: como una segunda antorcha a diferente temperatura
            Level.NETHER -> RGB(200, 50, 0, w = 30, c = 0, dimming = 70)

            // Más azul y oscuro: contraste frío/místico frente al morado del primario
            Level.END    -> RGB(50, 20, 200, w = 0, c = 0, dimming = 50)

            // Overworld: misma curva de día pero con offset de tiempo → ángulo de luz diferente
            else -> overworldColor(level.overworldClockTime + SECONDARY_TIME_OFFSET)
        }
    }
}