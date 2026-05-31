package me.fadel.ambientlights.client.lighting

import me.fadel.ambientlights.client.color.ColorInterpolator
import me.fadel.ambientlights.client.color.RGB
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes

object EnvironmentColorProvider {

    private const val SECONDARY_TIME_OFFSET = 600L

    // ── Fluids (highest priority) ─────────────────────────────────────────────
    private val water1 = RGB(0,  25,  160, w = 0,   c = 15,  dimming = 35)
    private val water2 = RGB(0,  15,  130, w = 0,   c = 8,   dimming = 28)
    private val lava1  = RGB(255, 90,   0, w = 220, c = 0,   dimming = 100)
    private val lava2  = RGB(235, 70,   0, w = 190, c = 0,   dimming = 95)

    // ── Overworld day cycle keyframes ─────────────────────────────────────────
    private val dawn         = RGB(210, 80,  0,  w = 200, c = 0,   dimming = 65)
    private val sunsetOrange = RGB(230, 90,  0,  w = 150, c = 0,   dimming = 95)
    private val duskDark     = RGB(80,  15,  5,  w = 5,   c = 0,   dimming = 22)
    private val night        = RGB(5,   10,  80, w = 0,   c = 0,   dimming = 20)

    // ── Overworld biome day colors ────────────────────────────────────────────
    private val dayDefault  = RGB(20,  60,  130, w = 100, c = 250, dimming = 100) // blue-white
    private val dayDesert   = RGB(90,  100,   5, w = 220, c = 180, dimming = 100) // warm yellow
    private val dayJungle   = RGB(10,  150,  40, w = 40,  c = 220, dimming = 100) // bright green
    private val daySnow     = RGB(100, 150, 220, w = 20,  c = 255, dimming = 100) // cool blue-white
    private val daySwamp    = RGB(20,   80,  40, w = 25,  c = 130, dimming = 85)  // muted green
    private val dayOcean    = RGB(10,   60, 160, w = 30,  c = 230, dimming = 100) // deep blue-white
    private val daySavanna  = RGB(120,  85,   5, w = 200, c = 150, dimming = 100) // warm orange

    // ── Nether biome colors (primary / secondary) ─────────────────────────────
    private val netherWastes1  = RGB(220, 20,   0, w = 20, c = 0,  dimming = 80)
    private val netherWastes2  = RGB(200, 50,   0, w = 30, c = 0,  dimming = 70)
    private val netherSoul1    = RGB(0,   50, 210, w = 0,  c = 30, dimming = 70) // soul fire
    private val netherSoul2    = RGB(10,  30, 180, w = 0,  c = 20, dimming = 60)
    private val netherCrimson1 = RGB(220, 10,  30, w = 5,  c = 0,  dimming = 80)
    private val netherCrimson2 = RGB(180, 30,  50, w = 0,  c = 0,  dimming = 70)
    private val netherWarped1  = RGB(0,  190, 150, w = 0,  c = 20, dimming = 65) // teal
    private val netherWarped2  = RGB(10, 160, 120, w = 0,  c = 10, dimming = 55)
    private val netherBasalt1  = RGB(70,  15,   5, w = 5,  c = 0,  dimming = 50) // dark gray
    private val netherBasalt2  = RGB(50,  10,  15, w = 0,  c = 0,  dimming = 40)

    // ── End biome colors (primary / secondary) ────────────────────────────────
    private val endDefault1      = RGB(90,  10, 180, w = 0, c = 0, dimming = 60)
    private val endDefault2      = RGB(50,  20, 200, w = 0, c = 0, dimming = 50)
    private val endHighlands1    = RGB(130,  0, 210, w = 0, c = 0, dimming = 70)
    private val endHighlands2    = RGB(160, 10, 220, w = 0, c = 0, dimming = 60)
    private val endMidlands1     = RGB(70,  15, 160, w = 0, c = 0, dimming = 55)
    private val endMidlands2     = RGB(90,  10, 190, w = 0, c = 0, dimming = 50)
    private val endBarrens1      = RGB(40,   5, 130, w = 0, c = 0, dimming = 45)
    private val endBarrens2      = RGB(20,  10, 140, w = 0, c = 0, dimming = 40)
    private val endSmallIslands1 = RGB(20,  20, 160, w = 0, c = 0, dimming = 50)
    private val endSmallIslands2 = RGB(40,  40, 170, w = 0, c = 0, dimming = 45)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun getBiomeKey(client: Minecraft): ResourceKey<Biome>? {
        val player = client.player ?: return null
        val level  = client.level  ?: return null
        return level.getBiome(player.blockPosition()).unwrapKey().orElse(null)
    }

    private fun biomeDayColor(key: ResourceKey<Biome>?): RGB = when (key) {
        Biomes.DESERT,
        Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS -> dayDesert

        Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE -> dayJungle

        Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.SNOWY_SLOPES,
        Biomes.FROZEN_RIVER, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN,
        Biomes.ICE_SPIKES -> daySnow

        Biomes.SWAMP, Biomes.MANGROVE_SWAMP -> daySwamp

        Biomes.OCEAN, Biomes.DEEP_OCEAN,
        Biomes.WARM_OCEAN, Biomes.COLD_OCEAN,
        Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.DEEP_COLD_OCEAN,
        Biomes.BEACH, Biomes.STONY_SHORE -> dayOcean

        Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA -> daySavanna

        else -> dayDefault
    }

    private fun overworldColor(dayTime: Long, biomeDay: RGB): RGB {
        val time = (dayTime % 24000).toInt()
        return when {
            time < 6000  -> ColorInterpolator.lerp(dawn,         biomeDay,     time / 6000f)
            time < 11000 -> biomeDay
            time < 13000 -> ColorInterpolator.lerp(biomeDay,     sunsetOrange, (time - 11000) / 2000f)
            time < 13600 -> ColorInterpolator.lerp(sunsetOrange, duskDark,     (time - 13000) / 600f)
            time < 14000 -> ColorInterpolator.lerp(duskDark,     night,        (time - 13600) / 400f)
            time < 22000 -> night
            else         -> ColorInterpolator.lerp(night,        dawn,         (time - 22000) / 2000f)
        }
    }

    private fun eyeFluidColor(client: Minecraft, secondary: Boolean): RGB? {
        val player = client.player ?: return null
        val level  = client.level  ?: return null
        val fluid  = level.getFluidState(BlockPos.containing(player.eyePosition))
        return when {
            fluid.`is`(FluidTags.LAVA)  -> if (secondary) lava2  else lava1
            fluid.`is`(FluidTags.WATER) -> if (secondary) water2 else water1
            else -> null
        }
    }

    private fun netherColor(biomeKey: ResourceKey<Biome>?, secondary: Boolean): RGB {
        val (c1, c2) = when (biomeKey) {
            Biomes.SOUL_SAND_VALLEY -> netherSoul1    to netherSoul2
            Biomes.CRIMSON_FOREST   -> netherCrimson1 to netherCrimson2
            Biomes.WARPED_FOREST    -> netherWarped1  to netherWarped2
            Biomes.BASALT_DELTAS    -> netherBasalt1  to netherBasalt2
            else                    -> netherWastes1  to netherWastes2
        }
        return if (secondary) c2 else c1
    }

    private fun endColor(biomeKey: ResourceKey<Biome>?, secondary: Boolean): RGB {
        val (c1, c2) = when (biomeKey) {
            Biomes.END_HIGHLANDS     -> endHighlands1    to endHighlands2
            Biomes.END_MIDLANDS      -> endMidlands1     to endMidlands2
            Biomes.END_BARRENS       -> endBarrens1      to endBarrens2
            Biomes.SMALL_END_ISLANDS -> endSmallIslands1 to endSmallIslands2
            else                     -> endDefault1      to endDefault2
        }
        return if (secondary) c2 else c1
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun primaryColor(client: Minecraft): RGB {
        eyeFluidColor(client, secondary = false)?.let { return it }
        val level    = client.level ?: return RGB(0, 0, 0, w = 150, c = 150, dimming = 100)
        val biomeKey = getBiomeKey(client)
        return when (level.dimension()) {
            Level.NETHER -> netherColor(biomeKey, secondary = false)
            Level.END    -> endColor(biomeKey, secondary = false)
            else         -> overworldColor(level.overworldClockTime, biomeDayColor(biomeKey))
        }
    }

    fun secondaryColor(client: Minecraft): RGB {
        eyeFluidColor(client, secondary = true)?.let { return it }
        val level    = client.level ?: return RGB(0, 0, 0, w = 200, c = 80, dimming = 95)
        val biomeKey = getBiomeKey(client)
        return when (level.dimension()) {
            Level.NETHER -> netherColor(biomeKey, secondary = true)
            Level.END    -> endColor(biomeKey, secondary = true)
            else         -> overworldColor(level.overworldClockTime + SECONDARY_TIME_OFFSET, biomeDayColor(biomeKey))
        }
    }
}
