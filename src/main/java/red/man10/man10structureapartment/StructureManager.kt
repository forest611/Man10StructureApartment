package red.man10.man10structureapartment

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import red.man10.man10structureapartment.Man10StructureApartment.Companion.instance
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min


object StructureManager {

    var distance = 64
    var maxApartCount = 128
    lateinit var world: World

    private var ownerData = HashMap<UUID,ApartData>()

    //  開いているマンションの数などを呼び出す
    private fun getApartCount():Int{
        return ownerData.size
    }

    private fun loadOwnerData(){

        ownerData.clear()

        val file = File("${instance.dataFolder.path}/OwnerData.json")

        if (!file.exists())return

        val reader = FileReader(file)

        ownerData = Gson().fromJson(reader.readText(),object : TypeToken<HashMap<UUID,ApartData>>(){}.type)

        reader.close()
    }

    private fun saveOwnerData(){

        val jsonStr = Gson().toJson(ownerData)

        val file = File("${instance.dataFolder.path}/OwnerData.json")

        val writer = FileWriter(file)

        writer.write(jsonStr)

        writer.close()
    }

    private fun set(data: ApartData){
        ownerData[data.owner] = data
        saveOwnerData()
    }

    //  ストラクチャーの保存
    fun save(owner:UUID){

        val manager = instance.server.structureManager
        val structure = manager.createStructure()

        val data = ownerData[owner]?:return

        val pos1 = data.pos1
        val pos2 = data.pos2

        structure.fill(pos1,pos2,true)

        val fileName = owner.toString()

        val file = File("${instance.dataFolder.path}/Apart/${fileName}")

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

        if (ownerData[owner]!=null)return

        //座標を仮設定
        var pos1 = Location(world,(getApartCount() * distance).toDouble(),60.0,0.0)

        //最大数に達していたら、一番古いアパートを削除する
        if (getApartCount()>= maxApartCount){
            val oldestData = ownerData.values.minByOrNull { it.lastAccess }!!
            remove(oldestData.owner)
            pos1 = oldestData.pos1
        }

        val manager = instance.server.structureManager
        val file = File("${instance.dataFolder.path}/Apart/${owner}")

        val structure = if (file.exists()){
            manager.loadStructure(file)
        } else {
            val default = File("${instance.dataFolder.path}/Apart/Default")
            if (!default.exists()){
                Bukkit.getLogger().warning("初期建築がありません")
                return
            }
            manager.loadStructure(default)
        }

        //TODO:要検証
        structure.place(pos1,true,StructureRotation.NONE,Mirror.NONE,-1,1F, Random())

        val pos2 = pos1.clone()

        pos2.x+=structure.size.x
        pos2.y+=structure.size.y
        pos2.z+=structure.size.z

        set(ApartData(owner,pos1,pos2, Date()))
    }

    fun remove(owner: UUID){

        val data = ownerData[owner]?:return

        val pos1 = data.pos1
        val pos2 = data.pos2

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

        ownerData.remove(owner)
        saveOwnerData()
    }
}

data class ApartData(
    val owner : UUID,
    val pos1 : Location,
    val pos2 : Location,
    val lastAccess : Date
)