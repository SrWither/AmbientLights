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

private const val WHITE: Int = 0xFFFFFFFF.toInt()
private const val GRAY: Int = 0xFF888888.toInt()
private const val SILVER: Int = 0xFFAAAAAA.toInt()
private const val GOLD: Int = 0xFFFFD700.toInt()
private const val CYAN: Int = 0xFF00CFFF.toInt()
private const val LAVENDER: Int = 0xFFBB88FF.toInt()

class LightConfigScreen(private val parent: Screen?) : Screen(Component.literal("AmbientLights")) {

    private lateinit var lightList: LightListWidget
    private var editButton: Button? = null
    private var toggleRoleButton: Button? = null
    private var removeButton: Button? = null

    override fun init() {
        lightList = LightListWidget(minecraft, width, height - 78, 32)
        addRenderableWidget(lightList)
        lightList.refresh(AmbientController.listLights())

        val btnY = height - 38
        addRenderableWidget(
            Button.builder(Component.literal("Add")) { minecraft.setScreen(AddEditLightScreen(this, null)) }
                .bounds(width / 2 - 172, btnY, 60, 20).build()
        )
        editButton = addRenderableWidget(
            Button.builder(Component.literal("Edit")) { openEditScreen() }
                .bounds(width / 2 - 107, btnY, 60, 20).build()
        )
        toggleRoleButton = addRenderableWidget(
            Button.builder(Component.literal("Toggle Role")) { toggleRole() }
                .bounds(width / 2 - 42, btnY, 90, 20).build()
        )
        removeButton = addRenderableWidget(
            Button.builder(Component.literal("Remove")) { removeSelected() }
                .bounds(width / 2 + 53, btnY, 65, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Done")) { onClose() }
                .bounds(width / 2 + 123, btnY, 50, 20).build()
        )
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)

        graphics.centeredText(font, title, width / 2, 10, WHITE)
        graphics.text(font, "Alias", width / 2 - 172, 22, GRAY)
        graphics.text(font, "IP", width / 2 - 52, 22, GRAY)
        graphics.text(font, "Type", width / 2 + 68, 22, GRAY)
        graphics.text(font, "Role", width / 2 + 110, 22, GRAY)

        val sel = lightList.selected
        editButton?.active = sel != null
        toggleRoleButton?.active = sel != null
        removeButton?.active = sel != null
    }

    fun refresh() = lightList.refresh(AmbientController.listLights())

    private fun openEditScreen() {
        lightList.selected?.info?.let { minecraft.setScreen(AddEditLightScreen(this, it)) }
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

    override fun onClose() = minecraft.setScreen(parent)

    // ── Scrollable list ───────────────────────────────────────────────────────

    inner class LightListWidget(mc: Minecraft, width: Int, height: Int, y: Int) :
        ObjectSelectionList<LightListWidget.LightEntry>(mc, width, height, y, 24) {

        inner class LightEntry(val info: LightInfo) : Entry<LightEntry>() {

            override fun getNarration() = Component.literal("${info.alias}: ${info.ip}")

            override fun extractContent(
                graphics: GuiGraphicsExtractor,
                mouseX: Int, mouseY: Int,
                hovered: Boolean, tickDelta: Float
            ) {
                val mc = Minecraft.getInstance()
                val x = contentX
                val y = contentYMiddle - mc.font.lineHeight / 2
                graphics.text(mc.font, info.alias, x + 10, y, WHITE)
                graphics.text(mc.font, info.ip, x + 115, y, SILVER)
                graphics.text(mc.font, info.type, x + 240, y, LAVENDER)
                val roleColor = if (info.role == LightRole.PRIMARY) GOLD else CYAN
                graphics.text(mc.font, info.role.name.lowercase(), x + 280, y, roleColor)
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
