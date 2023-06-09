package red.man10.man10structureapartment

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10structureapartment.StructureManager.jump
import red.man10.man10structureapartment.StructureManager.pluginLoad
import red.man10.man10structureapartment.StructureManager.saveStructure
import java.util.UUID
import java.util.concurrent.Executors

class Man10StructureApartment : JavaPlugin(),Listener {

    companion object{
        lateinit var instance : Man10StructureApartment

        private const val PERMISSION = "man10apart.op"

        fun msg(sender:Player?,str: String){
            sender?.sendMessage("§f§l[Cloud§7§lApartment]§f§l${str}")
        }
    }

    override fun onEnable() {
        // Plugin startup logic
        instance = this

        MenuFramework.setup(this)

        getCommand("msa")!!.setExecutor(this)
        server.pluginManager.registerEvents(this,this)
        server.pluginManager.registerEvents(MenuFramework.MenuListener,this)

        pluginLoad()
    }

    override fun onDisable() {
        // Plugin shutdown logic
        StructureManager.pluginClose()
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
                Thread{
                    pluginLoad()
                }.start()
            }

            "place" ->{
                if (!sender.hasPermission(PERMISSION))return true

                val uuid = UUID.fromString(args[1])

                val ret = StructureManager.placeStructure(uuid,sender.location)

                if (ret){
                    msg(sender,"設置完了しました。内容変更があった場合は/msa save ${uuid}を打ってください")
                }
            }

            "save" ->{
                if (!sender.hasPermission(PERMISSION))return true

                val uuid = UUID.fromString(args[1])

                saveStructure(uuid,op = true)

                msg(sender,"保存しました。建物は手動で削除してください。")
            }

            "pay" ->{
                val day = args[1].toInt()
                StructureManager.addPayment(sender,day)
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
        saveStructure(e.player.uniqueId)
    }

    //ドアクリックで飛ぶ
    @EventHandler
    fun clickEvent(e:PlayerInteractEvent){

        //オフハンドのクリックに反応しないように
        if (e.hand != EquipmentSlot.HAND)return

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        if (e.clickedBlock?.type != Material.IRON_DOOR)return

        jump(e.player)
    }
}