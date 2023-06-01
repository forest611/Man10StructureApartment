package red.man10.man10structureapartment

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.type.Door
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10structureapartment.StructureManager.jump
import red.man10.man10structureapartment.StructureManager.load
import red.man10.man10structureapartment.StructureManager.saveStructure
import java.util.concurrent.Executors

class Man10StructureApartment : JavaPlugin(),Listener {

    companion object{
        lateinit var instance : Man10StructureApartment
        private val threadPool = Executors.newSingleThreadExecutor()

        private const val PERMISSION = "man10apart.op"

        fun locToStr(loc:Location):String{
            return "${loc.world.name};${loc.x};${loc.y};${loc.z}"
        }
        fun strToLoc(str:String):Location{
            val array = str.split(";")
            return Location(Bukkit.getWorld(array[0]),array[1].toDouble(),array[2].toDouble(),array[3].toDouble())
        }

        fun msg(sender:Player,str: String){
            sender.sendMessage("§f§l[Cloud§7§lApartment]§f§l${str}")
        }
    }

    override fun onEnable() {
        // Plugin startup logic
        instance = this


        MenuFramework.setup(this)

        getCommand("msa")!!.setExecutor(this)
        server.pluginManager.registerEvents(this,this)
        server.pluginManager.registerEvents(MenuFramework.MenuListener,this)

        load()
    }

    override fun onDisable() {
        // Plugin shutdown logic
        threadPool.shutdown()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {

        if (sender !is Player)return true

        if (args.isNullOrEmpty()){

            MainMenu(sender).open()
            return true
        }


        when(args[0]){

            "reload" ->{
                if (!sender.hasPermission(PERMISSION))return true
                threadPool.execute {
                    load()
                }
            }

            "pay" ->{
                val day = args[1].toInt()
                threadPool.execute{
                    StructureManager.addPayment(sender,day)
                }
            }

            "jump" ->{
                jump(sender)
            }

        }

        return true
    }

    @EventHandler
    fun logout(e:PlayerQuitEvent){

        StructureManager.exit(e.player)

        threadPool.execute {
            saveStructure(e.player.uniqueId)
        }
    }

    //ドアクリックで飛ぶ
    @EventHandler
    fun clickEvent(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        if (e.clickedBlock?.type != Material.IRON_DOOR)return

        jump(e.player)
    }
}