package me.fadel.ambientlights.client.gui

import me.fadel.ambientlights.client.lighting.AmbientController
import me.fadel.ambientlights.client.lighting.AmbientController.LightInfo
import me.fadel.ambientlights.client.lighting.AmbientController.LightRole
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

private val WHITE  = 0xFFFFFFFF.toInt()
private val GRAY   = 0xFF888888.toInt()
private val GOLD   = 0xFFFFD700.toInt()
private val CYAN   = 0xFF00CFFF.toInt()

class LightConfigScreen(private val parent: Screen?) : Screen(Component.literal("AmbientLights")) {

    private lateinit var lightList: LightListWidget
    private var editIpButton: Button?     = null
    private var toggleRoleButton: Button? = null
    private var removeButton: Button?     = null

    override fun init() {
        lightList = LightListWidget(minecraft!!, width, height - 78, 32)
        addRenderableWidget(lightList)
        lightList.refresh(AmbientController.listLights())

        val btnY = height - 38
        addRenderableWidget(
            Button.builder(Component.literal("Add")) { minecraft?.setScreen(AddEditLightScreen(this, null)) }
                .bounds(width / 2 - 172, btnY, 60, 20).build()
        )
        editIpButton = addRenderableWidget(
            Button.builder(Component.literal("Edit IP")) { openEditScreen() }
                .bounds(width / 2 - 107, btnY, 70, 20).build()
        )
        toggleRoleButton = addRenderableWidget(
            Button.builder(Component.literal("Toggle Role")) { toggleRole() }
                .bounds(width / 2 - 32, btnY, 90, 20).build()
        )
        removeButton = addRenderableWidget(
            Button.builder(Component.literal("Remove")) { removeSelected() }
                .bounds(width / 2 + 63, btnY, 65, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Done")) { onClose() }
                .bounds(width / 2 + 133, btnY, 50, 20).build()
        )
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)

        graphics.centeredText(font, title, width / 2, 10, WHITE)
        graphics.text(font, "Alias",  width / 2 - 172, 22, GRAY)
        graphics.text(font, "IP",     width / 2 - 52,  22, GRAY)
        graphics.text(font, "Role",   width / 2 + 90,  22, GRAY)

        val sel = lightList.selected
        editIpButton?.active     = sel != null
        toggleRoleButton?.active = sel != null
        removeButton?.active     = sel != null
    }

    fun refresh() = lightList.refresh(AmbientController.listLights())

    private fun openEditScreen() {
        lightList.selected?.info?.let { minecraft?.setScreen(AddEditLightScreen(this, it)) }
    }

    private fun toggleRole() {
        val info = lightList.selected?.info ?: return
        val newRole = if (info.role == LightRole.PRIMARY) LightRole.SECONDARY else LightRole.PRIMARY
        AmbientController.editRole(info.alias, newRole)
        refresh()
    }

    private fun removeSelected() {
        val info = lightList.selected?.info ?: return
        AmbientController.removeLight(info.alias)
        refresh()
    }

    override fun onClose() = minecraft!!.setScreen(parent)

    // ── Scrollable list ──────────────────────────────────────────────────────

    inner class LightListWidget(mc: Minecraft, width: Int, height: Int, y: Int)
        : ObjectSelectionList<LightListWidget.LightEntry>(mc, width, height, y, 24) {

        inner class LightEntry(val info: LightInfo) : Entry<LightEntry>() {

            override fun getNarration() = Component.literal("${info.alias}: ${info.ip}")

            override fun extractContent(
                graphics: GuiGraphicsExtractor,
                mouseX: Int, mouseY: Int,
                hovered: Boolean, tickDelta: Float
            ) {
                val mc = Minecraft.getInstance()
                val x  = getContentX()
                val y  = getContentYMiddle() - mc.font.lineHeight / 2
                graphics.text(mc.font, info.alias,                x + 10,  y, WHITE)
                graphics.text(mc.font, info.ip,                   x + 130, y, 0xFFAAAAAA.toInt())
                val roleColor = if (info.role == LightRole.PRIMARY) GOLD else CYAN
                graphics.text(mc.font, info.role.name.lowercase(), x + 290, y, roleColor)
            }
        }

        fun refresh(lights: List<LightInfo>) {
            val selectedAlias = selected?.info?.alias
            clearEntries()
            lights.forEach { addEntry(LightEntry(it)) }
            selectedAlias?.let { alias ->
                children().find { it.info.alias == alias }?.let { setSelected(it) }
            }
        }

        override fun isFocused() = this@LightConfigScreen.focused == this
    }
}
