package me.fadel.ambientlights.client.config

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object LightConfig {

    private val gson = Gson()
    private val file: File = FabricLoader.getInstance().configDir.resolve("ambientlights.json").toFile()

    fun load(): List<String> {
        if (!file.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(file.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(ips: List<String>) {
        file.writeText(gson.toJson(ips))
    }
}
