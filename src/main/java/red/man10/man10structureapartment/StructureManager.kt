package red.man10.man10structureapartment

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import red.man10.man10structureapartment.Man10StructureApartment.Companion.instance
import java.io.File
import java.util.*
import kotlin.math.max
import kotlin.math.min


object StructureManager {


    fun getApartInfo(apartName: String){

    }

    //  ストラクチャーの保存
    //  ownerがnullの場合は、初期建築を保存する
    fun save(pos1:Location,pos2:Location,apartName:String,owner:UUID?){

        val manager = instance.server.structureManager
        val structure = manager.createStructure()

        structure.fill(pos1,pos2,true)

        val fileName = owner?.toString() ?: "Default"

        val file = File("${instance.dataFolder.path}/${apartName}/${fileName}")

        try {
            if (!file.exists()){
                manager.saveStructure(file,structure)
            }else{
                Bukkit.getLogger().info("上書きします")
                manager.saveStructure(file,structure)
            }
        }catch (e:Exception){
            Bukkit.getLogger().warning("書き込みエラー アパート名:${apartName} 持ち主:${owner}")
        }
    }

    //  ストラクチャーの呼び出し
    fun place(owner: UUID,apartName: String,pos:Location){
        val manager = instance.server.structureManager
        val file = File("${instance.dataFolder.path}/${apartName}/${owner}")

        val structure = if (file.exists()){
            manager.loadStructure(file)
        } else {
            val default = File("${instance.dataFolder.path}/${apartName}/Default")
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