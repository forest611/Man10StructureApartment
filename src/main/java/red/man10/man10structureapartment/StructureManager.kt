package red.man10.man10structureapartment

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import red.man10.man10structureapartment.Man10StructureApartment.Companion.instance
import java.io.File
import java.util.*
import kotlin.math.max
import kotlin.math.min


object StructureManager {

    var distance = 64
    lateinit var world: World


    //  開いているマンションの数などを呼び出す
    private fun getApartCount():Int{


        return 0
    }

    private fun getInfo(uuid:UUID):Pair<Location,Location>?{

        val pos1 = instance.config.getLocation("${uuid}.Pos1")
        val pos2 = instance.config.getLocation("${uuid}.Pos2")

        if (pos1 == null || pos2 == null)return null

        return Pair(pos1,pos2)
    }

    private fun setInfo(uuid:UUID,pos1:Location,pos2:Location){

        instance.config.set("${uuid}.Pos1",pos1)
        instance.config.set("${uuid}.Pos2",pos2)

        instance.saveConfig()
    }

    private fun removeInfo(uuid: UUID){
        instance.config.set("$uuid",null)
        instance.saveConfig()
    }

    //  ストラクチャーの保存
    fun save(owner:UUID){

        val manager = instance.server.structureManager
        val structure = manager.createStructure()

        val pair = getInfo(owner)?:return

        val pos1 = pair.first
        val pos2 = pair.second

        structure.fill(pos1,pos2,true)

        val fileName = owner.toString()

        val file = File("${instance.dataFolder.path}/${fileName}")

        try {
            if (!file.exists()){
                manager.saveStructure(file,structure)
            }else{
                Bukkit.getLogger().info("上書きします")
                manager.saveStructure(file,structure)
            }
        }catch (e:Exception){
            Bukkit.getLogger().warning("書き込みエラー 持ち主:${owner}")
        }
    }

    //  ストラクチャーの呼び出し
    fun place(owner: UUID){

        if (getInfo(owner)!=null)return

        val pos = Location(world,(getApartCount() * distance).toDouble(),60.0,0.0)

        val manager = instance.server.structureManager
        val file = File("${instance.dataFolder.path}/${owner}")

        val structure = if (file.exists()){
            manager.loadStructure(file)
        } else {
            val default = File("${instance.dataFolder.path}/Default")
            if (!default.exists()){
                Bukkit.getLogger().warning("初期建築がありません")
                return
            }
            manager.loadStructure(default)
        }

        //TODO:要検証
        structure.place(pos,true,StructureRotation.NONE,Mirror.NONE,-1,1F, Random())
    }

    fun remove(pos1:Location,pos2:Location){
        val world = pos1.world

        val minX = min(pos1.blockX,pos2.blockX)
        val minY = min(pos1.blockY,pos2.blockY)
        val minZ = min(pos1.blockZ,pos2.blockZ)
        val maxX = max(pos1.blockX,pos2.blockX)
        val maxY = max(pos1.blockY,pos2.blockY)
        val maxZ = max(pos1.blockZ,pos2.blockZ)

        //エンティティを削除
        for (e in world.entities){
            val loc = e.location
            if (loc.blockX in minX..maxX && loc.blockY in minY..maxY && loc.blockZ in minZ .. maxZ){
                e.remove()
            }
        }

        //ブロックを削除
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    world.getBlockAt(x, y, z).type = Material.AIR
                }
            }
        }
    }
}