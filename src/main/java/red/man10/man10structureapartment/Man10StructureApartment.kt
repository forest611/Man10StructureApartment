package red.man10.man10structureapartment

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class Man10StructureApartment : JavaPlugin() {

    companion object{
        lateinit var instance : Man10StructureApartment
    }

    override fun onEnable() {
        // Plugin startup logic
        instance = this
        getCommand("msa")!!.setExecutor(this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {

        if (sender !is Player)return true

        if (args.isNullOrEmpty()){
            return true
        }

        when(args[0]){

            "save"->{

            }

            "load"->{

            }


        }

        return true
    }

    fun loadApartment(p:Player){


    }
}