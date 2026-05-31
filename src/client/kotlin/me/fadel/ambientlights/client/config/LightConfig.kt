package me.fadel.ambientlights.client.config

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object LightConfig {

    data class LightEntry(val alias: String, val ip: String, val role: String)

    private val gson = Gson()
    private val file: File = FabricLoader.getInstance().configDir.resolve("ambientlights.json").toFile()

    fun load(): List<LightEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<LightEntry>>() {}.type
            gson.fromJson<List<LightEntry>>(file.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(entries: List<LightEntry>) {
        file.writeText(gson.toJson(entries))
    }
}
