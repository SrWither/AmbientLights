package me.fadel.ambientlights.client.gui

import me.fadel.ambientlights.client.lighting.AmbientController
import me.fadel.ambientlights.client.lighting.AmbientController.LightInfo
import me.fadel.ambientlights.client.lighting.AmbientController.LightRole
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class AddEditLightScreen(
    private val parent: LightConfigScreen,
    private val editing: LightInfo?
) : Screen(if (editing == null) Component.literal("Add Light") else Component.literal("Edit Light")) {

    private val isAdd = editing == null
    private var currentRole = editing?.role ?: LightRole.PRIMARY

    private var aliasBox: EditBox?  = null
    private var ipBox: EditBox?     = null
    private var roleButton: Button? = null
    private var saveButton: Button? = null

    override fun init() {
        val cx   = width / 2
        val base = height / 2 - if (isAdd) 55 else 40

        if (isAdd) {
            aliasBox = addRenderableWidget(
                EditBox(font, cx - 100, base, 200, 20, Component.empty()).also {
                    it.setHint(Component.literal("alias  (e.g. desk)"))
                    it.setMaxLength(32)
                }
            )
        }

        ipBox = addRenderableWidget(
            EditBox(font, cx - 100, base + if (isAdd) 30 else 0, 200, 20, Component.empty()).also {
                it.setHint(Component.literal("IP  (e.g. 192.168.1.50)"))
                it.setMaxLength(64)
                it.setValue(editing?.ip ?: "")
            }
        )

        val roleY = base + if (isAdd) 60 else 30
        roleButton = addRenderableWidget(
            Button.builder(roleLabel()) {
                currentRole = if (currentRole == LightRole.PRIMARY) LightRole.SECONDARY else LightRole.PRIMARY
                roleButton?.message = roleLabel()
                saveButton?.active = canSave()
            }.bounds(cx - 100, roleY, 200, 20).build()
        )

        val btnY = base + if (isAdd) 90 else 60
        saveButton = addRenderableWidget(
            Button.builder(Component.literal("Save")) { save() }
                .bounds(cx - 105, btnY, 100, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Cancel")) { onClose() }
                .bounds(cx + 5, btnY, 100, 20).build()
        )

        focused = if (isAdd) aliasBox else ipBox
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)

        val cx   = width / 2
        val base = height / 2 - if (isAdd) 55 else 40

        val WHITE  = 0xFFFFFFFF.toInt()
        val SILVER = 0xFFCCCCCC.toInt()
        val GRAY   = 0xFF888888.toInt()

        graphics.centeredText(font, title, cx, base - 20, WHITE)

        if (isAdd) {
            graphics.text(font, "Alias:", cx - 100, base - 10, SILVER)
            graphics.text(font, "IP:",    cx - 100, base + 20, SILVER)
        } else {
            graphics.text(font, "Alias: ${editing!!.alias}", cx - 100, base - 10, GRAY)
            graphics.text(font, "IP:",                        cx - 100, base - 10, SILVER)
        }

        saveButton?.active = canSave()
    }

    private fun roleLabel() = Component.literal(
        "Role: ${if (currentRole == LightRole.PRIMARY) "§eprimary" else "§bsecondary"}"
    )

    private fun canSave(): Boolean {
        val ipOk    = !ipBox?.getValue().isNullOrBlank()
        val aliasOk = !isAdd || !aliasBox?.getValue().isNullOrBlank()
        return ipOk && aliasOk
    }

    private fun save() {
        val ip = ipBox?.getValue()?.trim() ?: return
        if (ip.isEmpty()) return

        if (isAdd) {
            val alias = aliasBox?.getValue()?.trim() ?: return
            if (alias.isEmpty()) return
            AmbientController.addLight(alias, ip, currentRole)
        } else {
            AmbientController.editIp(editing!!.alias, ip)
            if (currentRole != editing.role) AmbientController.editRole(editing.alias, currentRole)
        }

        parent.refresh()
        minecraft?.setScreen(parent)
    }

    override fun onClose() { minecraft?.setScreen(parent) }
}
