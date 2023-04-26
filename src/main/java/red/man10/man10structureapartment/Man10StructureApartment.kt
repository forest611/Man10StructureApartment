package red.man10.man10structureapartment

import org.bukkit.plugin.java.JavaPlugin

class Man10StructureApartment : JavaPlugin() {

    companion object{
        lateinit var instance : Man10StructureApartment
    }

    override fun onEnable() {
        // Plugin startup logic
        instance = this
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}