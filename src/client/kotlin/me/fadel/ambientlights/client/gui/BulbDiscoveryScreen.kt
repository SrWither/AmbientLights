package me.fadel.ambientlights.client.gui

import me.fadel.ambientlights.client.bulb.BulbClientFactory
import me.fadel.ambientlights.client.bulb.BulbDiscovery.DiscoveredBulb
import me.fadel.ambientlights.client.color.RGB
import me.fadel.ambientlights.client.lighting.AmbientController
import me.fadel.ambientlights.client.lighting.EnvironmentColorProvider
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class BulbDiscoveryScreen(
    private val parent: AddEditLightScreen,
    private val type: String
) : Screen(Component.literal("Detect $type bulbs")) {

    private val results = mutableListOf<DiscoveredBulb>()
    private var status  = "Scanning…"
    private var started = false

    private lateinit var list:          DiscoveryList
    private var testButton: Button?     = null
    private var useButton:  Button?     = null

    override fun init() {
        list = DiscoveryList(minecraft!!, width, height - 80, 36)
        addRenderableWidget(list)

        val bw     = 50
        val gap    = 5
        val startX = width / 2 - (bw * 3 + gap * 2) / 2
        val btnY   = height - 28

        testButton = addRenderableWidget(
            Button.builder(Component.literal("Test")) { testSelected() }
                .bounds(startX, btnY, bw, 20).build()
        )
        useButton = addRenderableWidget(
            Button.builder(Component.literal("Use")) { useSelected() }
                .bounds(startX + bw + gap, btnY, bw, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Back")) { onClose() }
                .bounds(startX + (bw + gap) * 2, btnY, bw, 20).build()
        )

        if (!started) {
            started = true
            startDiscovery()
        }

        list.refresh(results)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)

        graphics.centeredText(font, title, width / 2, 14, 0xFFFFFFFF.toInt())
        graphics.centeredText(font, Component.literal(status), width / 2, height - 56, 0xFF888888.toInt())
        graphics.centeredText(
            font,
            Component.literal("§7Test blinks §cred §7— works best when no lights are already red"),
            width / 2, height - 42, 0xFF888888.toInt()
        )

        val hasSel = list.selected != null
        testButton?.active = hasSel
        useButton?.active  = hasSel
    }

    private fun useSelected() {
        val sel = list.selected?.bulb ?: return
        parent.fillFromDiscovery(sel.ip, sel.name)
        minecraft.setScreen(parent)
    }

    private fun testSelected() {
        val sel  = list.selected?.bulb ?: return
        val bulb = BulbClientFactory.create(type, sel.ip)

        testButton?.active = false

        Thread(null, {
            // Snapshot current state before flashing so we can restore it exactly
            val snapshot = bulb.readState()

            try {
                bulb.setColor(RGB(255, 0, 0, dimming = 100))
                Thread.sleep(1200)
            } catch (_: Exception) {}

            if (snapshot != null) {
                // Restore exactly what the bulb had before
                Thread(null, { bulb.setColor(snapshot) }, "BulbRestore")
                    .also { it.isDaemon = true }.start()
                Minecraft.getInstance().execute { testButton?.active = true }
            } else {
                // Fallback: restore from current environment (readState unsupported or timed out)
                Minecraft.getInstance().execute {
                    val mc      = Minecraft.getInstance()
                    val restore = if (mc.level != null) EnvironmentColorProvider.primaryColor(mc)
                                  else RGB(0, 0, 0, w = 150, c = 150, dimming = 100)
                    Thread(null, { bulb.setColor(restore) }, "BulbRestore")
                        .also { it.isDaemon = true }.start()
                    testButton?.active = true
                }
            }
        }, "BulbTest").also { it.isDaemon = true }.start()
    }

    override fun onClose() = minecraft.setScreen(parent)

    private fun startDiscovery() {
        val discovery = BulbClientFactory.createDiscovery(type)
        if (discovery == null) {
            status = "Type '$type' does not support detection"
            return
        }

        val existingIps = AmbientController.listLights().map { it.ip }.toSet()

        Thread(null, {
            discovery.discover(
                existingIps,
                onFound = { bulb ->
                    Minecraft.getInstance().execute {
                        results.add(bulb)
                        list.refresh(results)
                        status = "Found ${results.size}…"
                    }
                },
                onDone = { count ->
                    Minecraft.getInstance().execute {
                        status = if (count == 0) "No bulbs found" else "Found $count — select and press Use"
                    }
                }
            )
        }, "BulbDiscovery").also { it.isDaemon = true }.start()
    }

    // ── Discovery list ────────────────────────────────────────────────────────

    inner class DiscoveryList(mc: Minecraft, width: Int, height: Int, y: Int)
        : ObjectSelectionList<DiscoveryList.BulbEntry>(mc, width, height, y, 20) {

        override fun getRowWidth(): Int = this@BulbDiscoveryScreen.width - 12

        inner class BulbEntry(val bulb: DiscoveredBulb) : Entry<BulbEntry>() {

            override fun getNarration() = Component.literal("${bulb.name}: ${bulb.ip}")

            override fun extractContent(
                graphics: GuiGraphicsExtractor,
                mouseX: Int, mouseY: Int,
                hovered: Boolean, tickDelta: Float
            ) {
                val mc   = Minecraft.getInstance()
                val cx   = getContentX()
                val rowW = this@DiscoveryList.getRowWidth()
                val y    = getContentYMiddle() - mc.font.lineHeight / 2

                graphics.text(mc.font, bulb.name, cx + 10, y, 0xFFFFFFFF.toInt())
                val ipW = mc.font.width(bulb.ip)
                graphics.text(mc.font, bulb.ip, cx + rowW - 10 - ipW, y, 0xFFAAAAAA.toInt())
            }
        }

        fun refresh(bulbs: List<DiscoveredBulb>) {
            val sel = selected?.bulb?.ip
            clearEntries()
            bulbs.forEach { addEntry(BulbEntry(it)) }
            sel?.let { ip -> children().find { it.bulb.ip == ip }?.let { setSelected(it) } }
        }

        override fun isFocused() = this@BulbDiscoveryScreen.focused == this
    }
}
