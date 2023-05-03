package red.man10.man10structureapartment

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10structureapartment.StructureManager.distance
import red.man10.man10structureapartment.StructureManager.maxApartCount
import red.man10.man10structureapartment.StructureManager.world

class Man10StructureApartment : JavaPlugin(),Listener {

    companion object{
        lateinit var instance : Man10StructureApartment

        private const val PERMISSION = "man10apart.op"

        fun locToStr(loc:Location):String{
            return "${loc.world.name};${loc.x};${loc.y};${loc.z}"
        }
        fun strToLoc(str:String):Location{
            val array = str.split(";")
            return Location(Bukkit.getWorld(array[0]),array[1].toDouble(),array[2].toDouble(),array[3].toDouble())
        }
    }

    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()
        instance = this

        distance = config.getInt("Distance")
        maxApartCount = config.getInt("MaxApartCount")
        world = server.getWorld(config.getString("BuilderWorld")?:"world")!!

        getCommand("msa")!!.setExecutor(this)
        server.pluginManager.registerEvents(this,this)

        StructureManager.load()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {

        if (sender !is Player)return true

        if (args.isNullOrEmpty()){

            sender.sendMessage("""
                /msa save...建築の保存
                /msa place...建築の設置
                /msa remove...建築の削除
                /msa jump...建築にテレポート
                /msa default...初期建築の設定
            """.trimIndent())

            return true
        }

        if (!sender.hasPermission(PERMISSION))return true

        when(args[0]){

            "save"->{
                StructureManager.saveStructure(sender)
            }

            "place"->{
                StructureManager.placeStructure(sender)
            }

            "remove" ->{
                StructureManager.remove(sender.uniqueId)
            }

            "jump" ->{
                StructureManager.jump(sender)
            }

            "default" ->{
                sender.persistentDataContainer.set(NamespacedKey(this,"SetDefault"), PersistentDataType.INTEGER,1)
                sender.sendMessage("始点と終点を殴って範囲を指定する")
            }
        }

        return true
    }

    @EventHandler
    fun breakEvent(e:BlockBreakEvent){
        val p = e.player
        if (!p.hasPermission(PERMISSION))return

        val mode = p.persistentDataContainer[NamespacedKey(this,"SetDefault"), PersistentDataType.INTEGER] ?: return

        //POS1
        if (mode == 1){

            e.isCancelled = true

            p.persistentDataContainer
                .set(NamespacedKey(this,"Pos1"), PersistentDataType.STRING,locToStr(e.block.location))
            p.persistentDataContainer.set(NamespacedKey(this,"SetDefault"), PersistentDataType.INTEGER,2)
            p.sendMessage("始点を設定 終点を殴る")
            return
        }

        //POS2
        if (mode == 2){
            e.isCancelled = true

            val pos1Str = p.persistentDataContainer[NamespacedKey(this,"Pos1"), PersistentDataType.STRING]?:return
            val pos1 = strToLoc(pos1Str)
            val pos2 = e.block.location

            p.persistentDataContainer.remove(NamespacedKey(this,"Pos1"))
            p.persistentDataContainer.remove(NamespacedKey(this,"SetDefault"))


            StructureManager.saveDefault(p,pos1, pos2)
        }

        //Spawn
//        if (mode == 3){
//            e.isCancelled = true
//
//            val pos1Str = p.persistentDataContainer[NamespacedKey(this,"Pos1"), PersistentDataType.STRING]?:return
//            val pos2Str = p.persistentDataContainer[NamespacedKey(this,"Pos2"), PersistentDataType.STRING]?:return
//            val pos1 = strToLoc(pos1Str)
//            val pos2 = strToLoc(pos2Str)
//            val spawn = p.location
//
//            p.persistentDataContainer.remove(NamespacedKey(this,"Pos1"))
//            p.persistentDataContainer.remove(NamespacedKey(this,"Pos2"))
//            p.persistentDataContainer.remove(NamespacedKey(this,"SetDefault"))
//
//            p.sendMessage("スポーンを設定")
//
//            StructureManager.saveDefault(p,pos1, pos2,spawn)
//        }
    }
}